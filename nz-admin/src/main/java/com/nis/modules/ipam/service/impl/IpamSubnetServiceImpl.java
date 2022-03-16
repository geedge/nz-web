package com.nis.modules.ipam.service.impl;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.web.multipart.MultipartFile;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.CommonUtils;
import com.nis.common.utils.Constant;
import com.nis.common.utils.PageUtils;
import com.nis.common.utils.Query;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.ToolUtil;
import com.nis.modules.asset.entity.Dc;
import com.nis.modules.asset.service.DcService;
import com.nis.modules.ipam.dao.IpamSubnetDao;
import com.nis.modules.ipam.entity.IpamSubnet;
import com.nis.modules.ipam.service.IpamSubnetService;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import inet.ipaddr.AddressStringException;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import inet.ipaddr.IncompatibleAddressException;

@Service
public class IpamSubnetServiceImpl extends ServiceImpl<IpamSubnetDao, IpamSubnet> implements IpamSubnetService {

    protected Log log = Log.get();
    
    @Autowired
    private IpamSubnetDao ipamSubnetDao;
    
    @Autowired
    private SysConfigService sysConfigService;
    
    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;
    
    @Autowired
    private DcService dcService;

	@Override
	public IpamSubnet queryInfo(Integer id) {
		IpamSubnet ipamSubnet = ipamSubnetDao.selectById(id);
		Dc dc = dcService.getById(ipamSubnet.getDcId());
		ipamSubnet.setDc(dc);
		ipamSubnet.setUsed(0);
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
		
		IPage page = new Query(IpamSubnet.class).getPage(params);
		List<IpamSubnet> result = ipamSubnetDao.queryPage(page,params);
		
		result.stream().forEach(o -> o.setUsed(0));
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
		
		// 校验ip以及掩码值之前是否有创建或者包含
		String ip = StrUtil.builder(ipamSubnet.getAddr(),StrUtil.SLASH,ipamSubnet.getMask().toString()).toString();
		IPAddress ipAddress = new IPAddressString(ip).toAddress();
		List<IpamSubnet> ipamSubnets = this.list(new QueryWrapper<IpamSubnet>().lambda().ne(ObjectUtil.isNotEmpty(ipamSubnet.getId()),IpamSubnet::getId, ipamSubnet.getId()));
		List<String> ips = ipamSubnets.stream().map(o -> StrUtil.builder(o.getAddr(),StrUtil.SLASH,o.getMask().toString()).toString()).collect(Collectors.toList());
		if(ToolUtil.isNotAllEmpty(ips)) {
			for(String p : ips) {
				IPAddress ipAddr = new IPAddressString(p).toAddress();
				if(ipAddr.contains(ipAddress)) {
					throw new NZException(RCode.IPAM_SUBNET_INTERVAL_ERROR);
				}
			}
		}		
		this.saveOrUpdate(ipamSubnet);
		return ipamSubnet.getId();
	}

	@Override
	public void delIpamSubnets(List<Integer> ids) {
		ipamSubnetDao.deleteBatchIds(ids);
	}
	
	@Override
	public void getTemplateByLanguage(HttpServletResponse response,String format) throws IOException{
		String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_IPAMSUBNET_IMPORT_HEADER);
        String fileName = "IpamSubnetTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);
	}

	@Override
	public Map importIpamSubnetData(MultipartFile multipartFile) throws IOException, AddressStringException, IncompatibleAddressException {
		String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_IPAMSUBNET_IMPORT_HEADER);
		Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(multipartFile, sysHeaderJsonStr);

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();
        ArrayList<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());
        List<Map<Integer, String>> handleImportData = null;
        int successNum = this.saveIpamSubnetsFromImportDataList(importDataList, failDetails, new ArrayList<>(exportHeaderMap.keySet()), seqUUID);
        int totalNum = importDataList.size();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
	}

	private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
        Map failDetail = new HashMap(4);
        failDetail.put("lineNo", i);
        failDetail.put("errorMsg", headerInfo + ": " + msg);
        failDetails.add(failDetail);
    }
	
	private int saveIpamSubnetsFromImportDataList(List<Map<Integer, String>> importDataList, List<Map> failDetails, List<String> headerInfos, String seqUUID) throws AddressStringException, IncompatibleAddressException {
        int successNum = 0;
        // 校验ip以及掩码值之前是否有创建或者包含
 		List<IpamSubnet> ipamSubnets = this.list();
 		List<String> ips = ipamSubnets.stream().map(o -> StrUtil.builder(o.getAddr(),StrUtil.SLASH,o.getMask().toString()).toString()).collect(Collectors.toList());
 		
        for (int i = 0; i < importDataList.size(); i++) {
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            Map<Integer, String> dataMap = importDataList.get(i);

            String name = dataMap.get(0);
            if (StringUtils.isEmpty(name)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.IPAM_SUBNET_NAME_ISNULL.getMsg());
            }

            String type = dataMap.get(1);
            if(StringUtils.isEmpty(type)) {
            	saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.IPAM_SUBNET_TYPE_ISNULL.getMsg());
            }else {
            	if(!(StringUtils.isEquals(type, "4") || StringUtils.isEquals(type, "6"))) {
            		saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.IPAM_SUBNET_TYPE_ERROR.getMsg());
            	}
            }
            
            String addr = dataMap.get(2);
            
            
            
            if(StringUtils.isEmpty(addr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.IPAM_SUBNET_ADDR_ISNULL.getMsg());
            }else {
            	boolean ipFormat = true;
            	if(StringUtils.isEquals(type, "4")) {
            		ipFormat = CommonUtils.checkIpv4(addr);
            	}else if(StringUtils.isEquals(type, "6")){
            		ipFormat = CommonUtils.checkIpv6(addr);
            	}
            	if(!ipFormat) {
            		saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.IPAM_SUBNET_ADDR_ERROR.getMsg());
            	}
            }

            String mask = dataMap.get(3);
            Integer ipCap = null;
            if (StringUtils.isEmpty(mask)) {
            	saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.IPAM_SUBNET_MASK_ISNULL.getMsg());
            }else {
            	Integer maskInt = Integer.valueOf(mask);
            	boolean maskFormat = true;
            	if(StringUtils.isEquals(type, "4") && maskInt > 32) {
            		maskFormat = false;
            	}else if(StringUtils.isEquals(type, "4")){
            		ipCap = 32;
            	}else if(StringUtils.isEquals(type, "6") && maskInt > 128){
            		maskFormat = false;
            	}else if(StringUtils.isEquals(type, "6")) {
            		ipCap = 128;
            	}
            	if(!maskFormat) {
            		saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.IPAM_SUBNET_MASK_ERROR.getMsg());
            	}
            }

            // 校验ip以及掩码值之前是否有创建或者包含
            String ip = StrUtil.builder(addr,StrUtil.SLASH,mask).toString();
    		IPAddress ipAddress = new IPAddressString(ip).toAddress();
            if(ToolUtil.isNotAllEmpty(ips)) {
    			for(String p : ips) {
    				IPAddress ipAddr = new IPAddressString(p).toAddress();
    				if(ipAddr.contains(ipAddress)) {
    					saveFlag = false;
                        this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.IPAM_SUBNET_INTERVAL_ERROR.getMsg());
    				}
    			}
    		}		
            
            String vlan = dataMap.get(4);
            
            String dcName = dataMap.get(5);
            Integer dcId = null;
            if(StringUtils.isEmpty(dcName)) {
            	saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.IPAM_SUBNET_DC_ISNULL.getMsg());
            }else {
            	Dc dc = dcService.getOne(new QueryWrapper<Dc>().lambda().eq(Dc::getName, dcName));
            	if(ObjectUtils.isEmpty(dc)) {
            		saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.IPAM_SUBNET_DC_ISNULL.getMsg());
            	}else {
            		dcId= dc.getId();
            	}
            }
            
            String remark = dataMap.get(6);
            
            if (saveFlag) {
                IpamSubnet ipamSubnet = new IpamSubnet();
                try {
                	ipamSubnet.setName(name);
                    ipamSubnet.setType(Integer.valueOf(type));
                    ipamSubnet.setAddr(addr);
                    ipamSubnet.setMask(Integer.valueOf(mask));
                    ipamSubnet.setVlan(vlan);
                    ipamSubnet.setCap(new BigDecimal(Math.pow(2,ipCap - ipamSubnet.getMask())).toString());
                    ipamSubnet.setDcId(dcId);
                    ipamSubnet.setRemark(remark);
                	ipamSubnet.setSeq(seqUUID);
                    this.save(ipamSubnet);
                    successNum++;
                } catch (RuntimeException e) {
                    log.error(String.format("ipamsubnet 导入保存失败,实体信息是：%s,错误信息是：%s", JSONObject.toJSONString(ipamSubnet), e.getMessage()), e);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", RCode.EXCELFILE_IMPORT_ERROR.getMsg());
                }
            }
        }
        return successNum;
    }
		
	@Override
	public void export(HttpServletResponse response, Map<String, Object> params) throws IOException{
		String maxLine = sysConfigService.getValue(Constant.SYSCONFIG_KEY_EXPORT_MAX_LINE);
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
			tempList.add(ipamSubnet.getDc().getName());
			tempList.add(ipamSubnet.getRemark());
			tempList.add(ipamSubnet.getCap().toString());
			tempList.add(ipamSubnet.getUsed().toString());
			exportDataList.add(tempList);
		}
		String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_IPAMSUBNET_EXPORT_HEADER);
        String fileName = "Ipamsubnets";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
	}
	
	@Override
	public void cancelImport(String seq) {
		ipamSubnetDao.delete(new QueryWrapper<IpamSubnet>().lambda().eq(IpamSubnet::getSeq, seq));
	}
}
