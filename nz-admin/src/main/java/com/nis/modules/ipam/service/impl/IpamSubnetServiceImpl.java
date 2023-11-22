package com.nis.modules.ipam.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.ipam.dao.IpamIpDao;
import com.nis.modules.ipam.dao.IpamSubnetDao;
import com.nis.modules.ipam.entity.IpamIp;
import com.nis.modules.ipam.entity.IpamSubnet;
import com.nis.modules.ipam.entity.IpamSubnetEum;
import com.nis.modules.ipam.service.IpamSubnetService;
import com.nis.modules.sys.entity.SysUserStarredEntity;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.sys.service.SysUserStarredService;
import com.nis.modules.sys.shiro.ShiroUtils;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IpamSubnetServiceImpl extends ServiceImpl<IpamSubnetDao, IpamSubnet> implements IpamSubnetService {

    protected Log log = Log.get();
    
    @Autowired
    private IpamSubnetDao ipamSubnetDao;
    
    @Autowired
    private SysConfService sysConfService;
    
    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;
    
    @Autowired
    private DcService dcService;

	@Autowired
	private SysUserStarredService starredService;

	@Autowired
	private IpamIpDao ipamIpDao;

	@Override
	public IpamSubnet queryInfo(Integer id) {
		IpamSubnet ipamSubnet = ipamSubnetDao.selectById(id);
		if(Tool.ObjectUtil.isEmpty(ipamSubnet)){
			throw new NZException(RCode.IPAM_SUBNET_INTERVAL_ERROR);
		}
		Dc dc = dcService.getById(ipamSubnet.getDcId());
		ipamSubnet.setDc(dc);
		//查询使用数量
		long used = ipamIpDao.selectCount(new LambdaQueryWrapper<IpamIp>().eq(IpamIp::getSubnetId, ipamSubnet.getId()));
		ipamSubnet.setUsed(Long.valueOf(used).intValue());
		// 收藏标识 0：未收藏 1：已收藏
		List<SysUserStarredEntity> tUserStatList = starredService.list(new LambdaQueryWrapper<SysUserStarredEntity>()
				.select(SysUserStarredEntity::getId)
				.eq(SysUserStarredEntity::getTid, ipamSubnet.getId())
				.eq(SysUserStarredEntity::getType, "ipamSubnet")
				.eq(SysUserStarredEntity::getUid, ShiroUtils.getUserId())
				.last("limit 1")
		);
		ipamSubnet.setStarred(CollUtil.isEmpty(tUserStatList) ? 0 : 1);
		return ipamSubnet;
	}

	@Override
	public PageUtils queryPage(Map<String, Object> params) {
		
		if (ToolUtil.isNotEmpty(params.get("ids"))) {
            params.put("ids", Arrays.asList(String.valueOf(params.get("ids")).split(StringPool.COMMA)));
        }
		
		if (ToolUtil.isNotEmpty(params.get("dcIds"))) {
            params.put("dcIds", Arrays.asList(String.valueOf(params.get("dcIds")).split(StringPool.COMMA)));
        }

        // 当前登录用户
        params.put("currentLoginUserID", ShiroUtils.getUserId().intValue());

		IPage page = new Query(IpamSubnet.class).getPage(params);
		List<IpamSubnet> result = ipamSubnetDao.queryPage(page,params);
		page.setRecords(result);
        return new PageUtils(page);
	}

	@Override
	public Integer saveOrModifyIpamSubnet(IpamSubnet ipamSubnet) throws AddressStringException, IncompatibleAddressException {
		ValidateUtils.is(ipamSubnet.getName()).notNull(RCode.IPAM_SUBNET_NAME_ISNULL)
		.and(ipamSubnet.getType()).notNull(RCode.IPAM_SUBNET_TYPE_ISNULL)
		.and(ipamSubnet.getAddr()).notNull(RCode.IPAM_SUBNET_ADDR_ISNULL)
		.and(ipamSubnet.getMask()).notNull(RCode.IPAM_SUBNET_MASK_ISNULL)
		.and(ipamSubnet.getDcId()).notNull(RCode.IPAM_SUBNET_DCID_ISNULL);

		ipamSubnet.setName(StrUtil.trim(ipamSubnet.getName()));
		IpamSubnet one = ipamSubnetDao.selectOne(new LambdaQueryWrapper<IpamSubnet>().eq(IpamSubnet::getName, ipamSubnet.getName()));
		if ((Tool.ObjectUtil.isEmpty(ipamSubnet.getId()) && Tool.ObjectUtil.isNotEmpty(one))
				|| (Tool.ObjectUtil.isNotEmpty(ipamSubnet.getId()) && Tool.ObjectUtil.isNotEmpty(one) && !one.getId().equals(ipamSubnet.getId()))) {
			throw new NZException(RCode.IPAM_SUBNET_NAME_ERROR);
		}

		// 校验ip地址格式 以及掩码值
		boolean ipFormat = true;
		boolean maskFormat = true;
				
		Integer ipCap = null;
		if(ipamSubnet.getType().equals(4)) {
			ipCap=32;
			ipFormat = CommonUtils.checkIpv4(ipamSubnet.getAddr());
			if(ipamSubnet.getMask()>32) {
				maskFormat = false;
			}
		}else if(ipamSubnet.getType().equals(6)){
			ipCap=128;
			ipFormat = CommonUtils.checkIpv6(ipamSubnet.getAddr());
			if(ipamSubnet.getMask() > 128) {
				maskFormat = false;
			}
		}else {
			throw new NZException(RCode.IPAM_SUBNET_TYPE_ERROR);
		}
		if(!ipFormat){
			throw new NZException(RCode.IPAM_SUBNET_ADDR_ERROR);
		}
		if(!maskFormat){
			throw new NZException(RCode.IPAM_SUBNET_MASK_ERROR);
		}
		
		Dc dc = dcService.getById(ipamSubnet.getDcId());
		if(ToolUtil.isEmpty(dc)) {
			throw new NZException(RCode.IPAM_SUBNET_DC_ISNULL);
		}
		
		// 计算子网总容量
		ipamSubnet.setCap(new BigDecimal(Math.pow(2,ipCap-ipamSubnet.getMask())).toString());
		
		// 检验子网掩码是否修改
		String ip = StrUtil.builder(ipamSubnet.getAddr(),StrUtil.SLASH,ipamSubnet.getMask().toString()).toString();
		if (ObjectUtil.isNotEmpty(ipamSubnet.getId())){
			IpamSubnet cidr = this.getById(ipamSubnet.getId());
			String oidCidr = StrUtil.builder(cidr.getAddr(), StrUtil.SLASH, cidr.getMask().toString()).toString();
			if (! ip.equals(oidCidr)){
				throw new NZException(RCode.IPAM_SUBNET_MASK_NOT_MODIFIED);
			}
		}
		//校验子网网段是否重叠
		IPAddress ipAddress = new IPAddressString(ip).toAddress();
		List<IpamSubnet> ipamSubnets = this.list(new QueryWrapper<IpamSubnet>().lambda()
				.ne(ObjectUtil.isNotEmpty(ipamSubnet.getId()),IpamSubnet::getId, ipamSubnet.getId())
				.eq(IpamSubnet::getType, ipamSubnet.getType()));
		List<String> ips = ipamSubnets.stream().map(o -> StrUtil.builder(o.getAddr(),StrUtil.SLASH,o.getMask().toString()).toString()).collect(Collectors.toList());
		if(ToolUtil.isNotAllEmpty(ips)) {
			for(String p : ips) {
				IPAddress ipAddr = new IPAddressString(p).toAddress();
				IPAddress currentIpAddress = ipAddress.toPrefixBlock();
				IPAddress historyIpAddress = ipAddr.toPrefixBlock();
				if(currentIpAddress.contains(historyIpAddress) || historyIpAddress.contains(currentIpAddress)) {
					throw new NZException(RCode.IPAM_SUBNET_NETWORK_SEGMENT_OVERLAP.setParam(p));
				}
			}
		}		
		this.saveOrUpdate(ipamSubnet);
		return ipamSubnet.getId();
	}

	@Override
	@Transactional(rollbackFor = Exception.class)
	public R delIpamSubnets(Integer force, List<Integer> idList) {
		boolean forceData = false;
		List resultList = Tool.ListUtil.list(true);

		List<IpamSubnet> delIpamSubnets = this.list(new LambdaQueryWrapper<IpamSubnet>().in(IpamSubnet::getId, idList));
		if (Tool.CollUtil.isEmpty(delIpamSubnets)) {
			return  R.ok().putData("list", resultList);
		}

		for (IpamSubnet ipamSubnet : delIpamSubnets) {
			Map<Object, Object> relObjectInfoMap = this.getIpamSubnetDelRelObjectInfoMap(ipamSubnet);
			if (Tool.CollUtil.isNotEmpty((List) relObjectInfoMap.get("relates"))) forceData = true;
			resultList.add(relObjectInfoMap);
		}

		force = Tool.ObjectUtil.defaultIfNull(force, 0);
		switch (force) {
			case 0: {// 0：存在关联数据时，删除失败
				if (forceData) {
					return R.error(RCode.IPAM_SUBNET_RELOBJ_CAN_NOT_REMOVE).putData("list", resultList);
				}
			}
			case 1:  // 1：存在关联数据时，同时删除关联数据
				break;
			default:
				throw new NZException(RCode.ASSET_FORCE_INVALIDE);
		}

		// 删除自身和关联内容
		// ipamSubnet
		ipamSubnetDao.deleteBatchIds(idList);

		// ipam ip
		List<IpamIp> ipamIpList = ipamIpDao.selectList(new LambdaQueryWrapper<IpamIp>().in(IpamIp::getSubnetId, idList));
		if (Tool.CollUtil.isNotEmpty(ipamIpList)) {
			List<Integer> removeIpamIdList = ipamIpList.stream().map(IpamIp::getId).collect(Collectors.toList());
			ipamIpDao.deleteBatchIds(removeIpamIdList);
		}

		// starred
		starredService.remove(
				new LambdaQueryWrapper<SysUserStarredEntity>()
						.eq(SysUserStarredEntity::getType, "ipamSubnet")
						.in(SysUserStarredEntity::getTid, idList)
		);
		return R.ok().putData("list", resultList);
	}

	/**
	 * 获取 subnet 删除提示关联信息：
	 * ipam ip detail
	 *
	 * @return
	 */
	private Map<Object, Object> getIpamSubnetDelRelObjectInfoMap(IpamSubnet subnet) {
		List<Map> relatesList = Tool.ListUtil.list(true);

		// ipam ip
		List<IpamIp> ipamIpList = ipamIpDao.selectList(new LambdaQueryWrapper<IpamIp>().eq(IpamIp::getSubnetId, subnet.getId()));
		if (Tool.CollUtil.isNotEmpty(ipamIpList)) {
			for (IpamIp ipamIp : ipamIpList) {
				Map<Object, Object> map = Tool.MapUtil.builder()
						.put("id", ipamIp.getId())
						.put("name", ipamIp.getName())
						.put("type", "ipamIp")
						.build();
				relatesList.add(map);
			}
		}

		Map<Object, Object> delRelObjectMap = Tool.MapUtil.builder()
				.put("id", subnet.getId())
				.put("name", subnet.getName())
				.put("type", "ipamSubnet")
				.put("relates", relatesList)
				.build();

		return delRelObjectMap;
	}

	@Override
	public void getTemplateByLanguage(HttpServletResponse response,String format) throws IOException{
		String sysHeaderJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_IPAMSUBNET_IMPORT_HEADER);
        String fileName = "IpamSubnetTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
	}

	@Override
	public Map importIpamSubnetData(MultipartFile multipartFile,Map<String,Object> params) throws IOException, AddressStringException, IncompatibleAddressException {
		String sysHeaderJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_IPAMSUBNET_IMPORT_HEADER);
		Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);
		// 表头信息 国际化之后的
		List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

		String fileExtension = FilenameUtils.getExtension(multipartFile.getOriginalFilename());
		params.put("fileExtension", fileExtension);

		// 返回统计信息
		Map statMap = this.saveIpamSubnetsFromImportDataList(importDataList, headerInfos, params);
		return statMap;
	}
	
	private Map saveIpamSubnetsFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String,Object> params) throws AddressStringException, IncompatibleAddressException {
		String seq = StrUtil.uuid();
		params.put("seq", seq);
		// 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
		String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

		// 创建导入校验对象
		ImportValidateUtil validateUtil = ImportValidateUtil.builder()
				.existed(existed).headerInfos(headerInfos)
				.build();
		Integer addNum = 0, updateNum = 0;
		// 记录导入失败内容
		List<Map<Integer, String>> failContentList = ListUtil.list(true);
        for (int i = 0; i < importDataList.size(); i++) {
			int lineNum = i + 1;
            Map<Integer, String> dataMap = importDataList.get(i);

            //name 校验
            String name = dataMap.get(IpamSubnetEum.NAME.getIndex());
			validateUtil.validateRequired(name, lineNum, IpamSubnetEum.NAME.getIndex());
			boolean indexDuplicate = validateUtil.validateIndexDuplicate("ipam_subnet", "name", name, lineNum, IpamSubnetEum.NAME.getIndex());
			// 索引列重复 & 忽略标识，提示忽略本行
			if (BooleanUtil.and(indexDuplicate, StrUtil.equals("2" , existed))){
				validateUtil.ignoreThisRow(lineNum, IpamSubnetEum.NAME.getIndex());
				failContentList.add(dataMap);
				continue;
			}

			//ip 类型校验
            String type = dataMap.get(IpamSubnetEum.TYPE.getIndex());
			validateUtil.validateRequired(type, lineNum, IpamSubnetEum.TYPE.getIndex());
			validateUtil.validateEnumValArr(type, Arrays.asList("4","6"), lineNum, IpamSubnetEum.TYPE.getIndex());

			//ip校验
            String addr = dataMap.get(IpamSubnetEum.ADDR.getIndex());
			validateUtil.validateRequired(addr, lineNum, IpamSubnetEum.ADDR.getIndex());
			validateUtil.validateIp(addr, lineNum, IpamSubnetEum.ADDR.getIndex(), type);

			//掩码校验
            String mask = dataMap.get(IpamSubnetEum.MASK.getIndex());
			validateUtil.validateRequired(mask, lineNum, IpamSubnetEum.MASK.getIndex());
			Integer ipCap = validateUtil.validateMask(mask, type, lineNum, IpamSubnetEum.MASK.getIndex());

            // 校验子网网段是否重叠
			List<IpamSubnet> ipamSubnets = this.list(new LambdaQueryWrapper<IpamSubnet>().eq(IpamSubnet::getType, type));
			String cidr = StrUtil.concat(true, addr, StrUtil.SLASH, mask);
			IPAddress ipAddress = new IPAddressString(cidr).toAddress();
			Map<String, String> cidrByName = ipamSubnets.stream().collect(Collectors.toMap(subnet -> subnet.getName(), subnet -> StrUtil.concat(true, subnet.getAddr(), StrUtil.SLASH, subnet.getMask().toString()).toString()));
			if (ToolUtil.isNotAllEmpty(cidrByName)) {
				for (String subnetName : cidrByName.keySet()) {
					String historyCidr = cidrByName.get(subnetName);
					IPAddress ipAddr = new IPAddressString(historyCidr).toAddress();
					IPAddress currentIpAddress = ipAddress.toPrefixBlock();
					IPAddress historyIpAddress = ipAddr.toPrefixBlock();
					if ((currentIpAddress.contains(historyIpAddress) || historyIpAddress.contains(currentIpAddress)) && !subnetName.equalsIgnoreCase(name)) {
						validateUtil.recordImportAttrErr(lineNum, RCode.IPAM_SUBNET_NETWORK_SEGMENT_OVERLAP.setParam(historyCidr));
					}
				}
			}
            //dc相关校验
            String dcId = dataMap.get(IpamSubnetEum.DC.getIndex());
			validateUtil.validateRequired(dcId, lineNum, IpamSubnetEum.DC.getIndex());
			Dc dc = dcService.getById(dcId);
			if (ObjectUtil.isEmpty(dc)){
				validateUtil.recordImportAttrErr(lineNum, RCode.IPAM_SUBNET_DC_ISNULL);
			}
			// 遇到错误是否继续导入，可选值： 0：不继续    1：继续    默认值：1
			String ignoreError = Tool.ObjectUtil.defaultIfEmpty((String) params.get("ignoreError"), "1");
			List<Map<String, String>> errMsgForThisLine = validateUtil.getImportErrMsgForThisLine(lineNum);
			if (BooleanUtil.and(Tool.StrUtil.equals("0", ignoreError), Tool.CollUtil.isNotEmpty(errMsgForThisLine))) {
				// 结束本次导入，自导入行开始 到结尾数据行，提示错误信息
				for (int j = i; j < importDataList.size(); j++) {
					validateUtil.recordImportAttrErr(j + 1, RCode.IMPORT_ERROR_STOP);
					failContentList.add(importDataList.get(j));
				}
				break;
			}
			if (Tool.CollUtil.isEmpty(errMsgForThisLine)) {
				String vlan = dataMap.get(IpamSubnetEum.VLAN.getIndex());
				String remark = dataMap.get(IpamSubnetEum.REMARK.getIndex());

				IpamSubnet ipamSubnet = new IpamSubnet();
				ipamSubnet.setName(name);
				ipamSubnet.setType(Integer.valueOf(type));
				ipamSubnet.setAddr(addr);
				ipamSubnet.setMask(Integer.valueOf(mask));
				ipamSubnet.setVlan(vlan);
				ipamSubnet.setCap(new BigDecimal(Math.pow(2,ipCap - ipamSubnet.getMask())).toString());
				ipamSubnet.setRemark(remark);
				ipamSubnet.setSeq(params.get("seq").toString());
				ipamSubnet.setDcId(dc.getId());
				try {
					this.saveOrUpdateRowData(ipamSubnet,params);
					// save or update
					Object saveFlag = params.get("save");
					if (ObjectUtil.isNotNull(saveFlag)) {
						addNum++;
					} else {
						updateNum++;
					}
				} catch (Exception e) {
					failContentList.add(dataMap);
					validateUtil.recordImportAttrErr(lineNum, RCode.ERROR);
					log.error("ipamSubnet import roll back the transaction failure", e);
				}
			} else {
				failContentList.add(dataMap);
			}
		}
		String sysHeaderJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_IPAMSUBNET_IMPORT_HEADER);
		String failedContentFileBase64Str = basicImportAndExportServices.getFailedContentFileBase64Str(failContentList, sysHeaderJsonStr, MapUtil.getStr(params, "fileExtension"));
		//  返回统计信息
		Map<Object, Object> statMap = Tool.MapUtil.builder()
				.put("seq", seq)
				.put("totalNum", importDataList.size())
				.put("successNum", addNum + updateNum)
				.put("failNum", failContentList.size())
				.put("addNum", addNum)
				.put("updateNum", updateNum)
				.put("failDetail", validateUtil.getImportErrMsg())
				.put("failContent", failedContentFileBase64Str)
				.build();
		return statMap;
    }

	private void saveOrUpdateRowData(IpamSubnet ipamSubnet, Map<String,Object> params) {
		MapUtil.removeAny(params, "save", "update");
		// 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
		String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

		IpamSubnet oldIpamSubnet = this.getOne(new LambdaQueryWrapper<IpamSubnet>().eq(IpamSubnet::getName, ipamSubnet.getName()));
		if (StrUtil.equals("1", existed) && oldIpamSubnet != null) {
			params.put("update", true);
			// update
			ipamSubnet.setId(oldIpamSubnet.getId());

			LambdaUpdateWrapper<IpamSubnet> updateWrapper = new LambdaUpdateWrapper<IpamSubnet>()
					.eq(IpamSubnet::getId, ipamSubnet.getId())
					.set(IpamSubnet::getName, ipamSubnet.getName())
					.set(IpamSubnet::getAddr, ipamSubnet.getAddr())
					.set(IpamSubnet::getMask, ipamSubnet.getMask())
					.set(IpamSubnet::getCap, ipamSubnet.getCap())
					.set(IpamSubnet::getDcId, ipamSubnet.getDcId())
					.set(IpamSubnet::getRemark, ipamSubnet.getRemark())
					.set(IpamSubnet::getSeq, ipamSubnet.getSeq());
			this.update(ipamSubnet, updateWrapper);
		} else {
			params.put("save", true);
			// insert
			this.save(ipamSubnet);
		}
	}

	@Override
	public void export(HttpServletResponse response, Map<String, Object> params) throws IOException{
		String maxLine = sysConfService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
        long exportMaxLine = Integer.parseInt(StrUtil.emptyToDefault(maxLine, "10000"));
        // 导出最大条数限制
        Object limit = params.get(Constant.LIMIT) == null ? Constant.PAGESIZE : params.get(Constant.LIMIT);
        Long pageSize = Long.parseLong(limit.toString());
        if (pageSize == -1) {
            // 导出所有直接限制 pageSize
            params.put(Constant.PAGE, "1");
            params.put(Constant.LIMIT, Long.toString(exportMaxLine));
        } else if (pageSize > exportMaxLine) {
            // 分页超过最大限制 则设置最大限制为 系统配置值，和 pageSize == -1 判断区别在于 pageNo
            params.put(Constant.LIMIT, Long.toString(exportMaxLine));
        }
		PageUtils page = this.queryPage(params);
		List<IpamSubnet> result = (List<IpamSubnet>) page.getList();
		List<List<String>> exportDataList = new ArrayList<>();

		for (IpamSubnet ipamSubnet : result) {
			List<String> tempList = new ArrayList<>();
			tempList.add(ipamSubnet.getName());
			tempList.add(ipamSubnet.getType().toString());
			tempList.add(ipamSubnet.getAddr());
			tempList.add(ipamSubnet.getMask().toString());
			tempList.add(ipamSubnet.getVlan());
			tempList.add(ipamSubnet.getDc().getId().toString());
			tempList.add(ipamSubnet.getRemark());
			exportDataList.add(tempList);
		}
		String sysHeaderJsonStr = sysConfService.getValue(Constant.SYSCONFIG_KEY_IPAMSUBNET_IMPORT_HEADER);
        String fileName = "Ipamsubnets";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
	}
	
	@Override
	public void cancelImport(String seq) {
		ipamSubnetDao.delete(new QueryWrapper<IpamSubnet>().lambda().eq(IpamSubnet::getSeq, seq));
	}
}
