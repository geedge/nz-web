package com.nis.modules.ipam.service.impl;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.dao.AssetAssetDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.ipam.dao.IpamIpDao;
import com.nis.modules.ipam.entity.IpamIp;
import com.nis.modules.ipam.entity.IpamIpEum;
import com.nis.modules.ipam.entity.IpamSubnet;
import com.nis.modules.ipam.service.IpamIpService;
import com.nis.modules.ipam.service.IpamSubnetService;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class IpamIpServiceImpl extends ServiceImpl<IpamIpDao, IpamIp> implements IpamIpService {

    protected Log log = Log.get();

    @Autowired
    private IpamSubnetService ipamSubnetService;

    @Autowired
    private AssetAssetService asset;

    @Autowired
    private IpamIpDao ipamIpDao;

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private BasicImportAndExportServices basicImportAndExportServices;

    @Autowired
    private AssetAssetDao assetAssetDao;

    @Override
    public IpamIp ipamIpInfo(Integer id) {

        IpamIp ipamIp = this.getById(id);
        if (Tool.ObjectUtil.isEmpty(ipamIp)) {
            throw new NZException(RCode.IPAM_IP_INTERVAL_ERROR);
        }
        Integer subnetId = ipamIp.getSubnetId();
        IpamSubnet ipamSubnet = ipamSubnetService.getById(subnetId);
        ipamIp.setIpamSubnet(ipamSubnet);
        AssetAsset asset = this.asset.getById(ipamIp.getAssetId());
        ipamIp.setAsset(asset);
        return ipamIp;
    }

    @Override
    public PageUtils ipamIpList(Map params) {
        if (ToolUtil.isNotEmpty(params.get("ids"))) {
            params.put("ids", Arrays.asList(String.valueOf(params.get("ids")).split(StringPool.COMMA)));
        }
        IPage page = new Query(IpamIp.class).getPage(params);

        List<IpamIp> result = ipamIpDao.queryPage(page,params);

        page.setRecords(result);

        return new PageUtils(page);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer saveOrModifyIpamSubnet(IpamIp ipamIp) throws UnknownHostException {
        ValidateUtils.is(ipamIp.getAddr()).notNull(RCode.IPAM_IP_ADDR_ISNULL)
                .and(ipamIp.getName()).notNull(RCode.IPAM_IP_NAME_ISNULL);

        //判断name是否重复
        ipamIp.setName(StrUtil.trim(ipamIp.getName()));
        IpamIp selectOne = ipamIpDao.selectOne(new LambdaQueryWrapper<IpamIp>().eq(IpamIp::getName, ipamIp.getName()));
        if ((Tool.ObjectUtil.isEmpty(ipamIp.getId()) && Tool.ObjectUtil.isNotEmpty(selectOne))
                || (Tool.ObjectUtil.isNotEmpty(ipamIp.getId()) && Tool.ObjectUtil.isNotEmpty(selectOne) && !selectOne.getId().equals(ipamIp.getId()))) {
            throw new NZException(RCode.IPAM_IP_NAME_ERROR);
        }
        AssetAsset assetAsset = assetAssetDao.selectOne(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getManageIp, ipamIp.getAddr()));
        if (Tool.ObjectUtil.isNotEmpty(assetAsset)) {
            ipamIp.setAssetId(assetAsset.getId());
        }

        //校验ip
        if (!(CommonUtils.checkIpv4(ipamIp.getAddr()) || CommonUtils.checkIpv6(ipamIp.getAddr()))){
            throw new NZException(RCode.IPAM_IP_ADDR_ERROR);
        }
        //校验ip是否重复
        List<IpamIp> ipamIpList = ipamIpDao.selectList(new LambdaQueryWrapper<IpamIp>().ne(ObjectUtil.isNotEmpty(ipamIp.getId()), IpamIp::getId, ipamIp.getId()));
        List<String> ipList = ipamIpList.stream().map(ipam -> ipam.getAddr()).collect(Collectors.toList());
        if (ToolUtil.isNotAllEmpty(ipList)) {
            if (ipList.contains(ipamIp.getAddr())) {
                throw new NZException(RCode.IPAM_IP_INTERVAL_ERROR);
            }
        }
        //判断ip属于那个子网
        boolean inRange = true;
        List<IpamSubnet> subnets = ipamSubnetService.list();
        for (IpamSubnet subnet : subnets) {
            String cidr = StrUtil.builder(subnet.getAddr(),StrUtil.SLASH,subnet.getMask().toString()).toString();
            CIDRUtils cidrUtils = new CIDRUtils(cidr);
            inRange = cidrUtils.isInRange(ipamIp.getAddr());
            if (inRange){
                ipamIp.setSubnetId(subnet.getId());
                break;
            }
        }
        if (!inRange){
            throw new NZException(RCode.IPAM_IP_SUBNET_ISNULL);
        }
        if (Tool.ObjectUtil.isEmpty(ipamIp.getState())){
            ipamIp.setState(1);
        }
        ipamIp.setUts(new Date().getTime());
        this.saveOrUpdate(ipamIp);
        return ipamIp.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delIpamIps(List<Integer> asList) {
        this.removeByIds(asList);
    }

    @Override
    public void getTemplateByLanguage(HttpServletResponse response, String format) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_IPAMIP_IMPORT_HEADER);
        String fileName = "IpamIpTemplate";
        basicImportAndExportServices.getTemplate(sysHeaderJsonStr, format, fileName, response);

    }

    @Override
    public Map importIpamIpData(MultipartFile excelFile, Map<String, Object> params) throws IOException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_IPAMIP_IMPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(excelFile, sysHeaderJsonStr);

        // 表头信息 国际化之后的
        List<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());

        // 返回统计信息
        Map statMap = this.saveIpamIpsFromImportDataList(importDataList, headerInfos, params);
        return statMap;
    }

    private Map saveIpamIpsFromImportDataList(List<Map<Integer, String>> importDataList, List<String> headerInfos, Map<String, Object> params) throws UnknownHostException {
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

            //name
            String name = dataMap.get(IpamIpEum.NAME.getIndex());
            validateUtil.validateRequired(name, lineNum, IpamIpEum.NAME.getIndex());
            boolean indexDuplicate = validateUtil.validateIndexDuplicate("ipam_ip", "name", name, lineNum, IpamIpEum.NAME.getIndex());
            // 索引列重复 & 忽略标识，提示忽略本行
            if (BooleanUtil.and(indexDuplicate, StrUtil.equals("2" , existed))){
                validateUtil.ignoreThisRow(lineNum, IpamIpEum.NAME.getIndex());
                failContentList.add(dataMap);
                continue;
            }

            String subnetName = dataMap.get(IpamIpEum.SUBNET.getIndex());
            validateUtil.validateRequired(subnetName, lineNum, IpamIpEum.SUBNET.getIndex());
            validateUtil.validateRelObjExits("ipam_subnet", "name", subnetName , false, lineNum , IpamIpEum.SUBNET.getIndex());

            String addr = dataMap.get(IpamIpEum.ADDR.getIndex());
            IpamSubnet ipamSubnet = ipamSubnetService.getOne(new LambdaQueryWrapper<IpamSubnet>().eq(IpamSubnet::getName, subnetName));
            validateUtil.validateRequired(addr, lineNum, IpamIpEum.ADDR.getIndex());
            validateUtil.validateIp(addr, lineNum , IpamIpEum.ADDR.getIndex());
            validateUtil.validateIpInScope(addr, ipamSubnet.getAddr(),ipamSubnet.getMask().toString(), lineNum, IpamIpEum.ADDR.getIndex());

            String mac = dataMap.get(IpamIpEum.MAC.getIndex());
            validateUtil.validateMac(mac, lineNum, IpamIpEum.MAC.getIndex());

            // 校验ip之前是否有创建或者包含
            validateUtil.validateDuplicate("ipam_ip", "addr", addr,name , lineNum, IpamIpEum.ADDR.getIndex());

            String assetName = dataMap.get(IpamIpEum.ASSET.getIndex());
            Integer state = null;
            if(StringUtils.isEmpty(assetName)){
                //资产不存在 当前IP为未分配状态
                state = 1;
            } else {
                Boolean aBoolean = validateUtil.validateRelObjExits("asset_asset", "name", assetName, false, lineNum, IpamIpEum.ASSET.getIndex());
                if (aBoolean){
                    //资产存在 当前IP为已分配状态
                    state = 3;
                }else {
                    //资产不存在 当前IP为未分配状态
                    state = 1;
                }
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
                AssetAsset assetAsset = asset.getOne(new LambdaQueryWrapper<AssetAsset>().eq(AssetAsset::getName, assetName));
                String remark = dataMap.get(IpamIpEum.REMARK.getIndex());
                IpamIp ipamIp = new IpamIp();
                ipamIp.setName(name);
                ipamIp.setSubnetId(ipamSubnet.getId());
                ipamIp.setAddr(addr);
                ipamIp.setMac(mac);
                if (ObjectUtil.isEmpty(assetAsset)){
                    ipamIp.setAssetId(null);
                }else {
                    ipamIp.setAssetId(assetAsset.getId());
                }
                ipamIp.setRemark(remark);
                ipamIp.setSeq(params.get("seq").toString());
                ipamIp.setState(state);
                ipamIp.setUts(System.currentTimeMillis());
                try {
                    this.saveOrUpdateRowData(ipamIp,params);
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
                    log.error("ipamIp import roll back the transaction failure", e);
                }
            } else {
                failContentList.add(dataMap);
            }
        }
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_IPAMIP_IMPORT_HEADER);
        String failedContentFileBase64Str = basicImportAndExportServices.getFailedContentFileBase64Str(failContentList, sysHeaderJsonStr, Constant.ImportFileType.EXCEL.getValue());
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

    private void saveOrUpdateRowData(IpamIp ipamIp, Map<String, Object> params) {
        MapUtil.removeAny(params, "save", "update");
        // 已存在的内容处理方式，可选值：0：错误 error  1：覆盖 override   2：忽略 ignore
        String existed = Tool.ObjectUtil.defaultIfEmpty((String) params.get("existed"), "1");

        IpamIp oldIIpamIp = this.getOne(new LambdaQueryWrapper<IpamIp>().eq(IpamIp::getName, ipamIp.getName()));
        if (StrUtil.equals("1", existed) && oldIIpamIp != null) {
            params.put("update", true);
            // update
            ipamIp.setId(oldIIpamIp.getId());
            this.updateById(ipamIp);
        } else {
            params.put("save", true);
            // insert
            this.save(ipamIp);
        }
    }

    @Override
    public void export(HttpServletResponse response, Map<String, Object> params) throws IOException {
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
        PageUtils page = this.ipamIpList(params);
        List<IpamIp> result = (List<IpamIp>) page.getList();
        List<List<String>> exportDataList = new ArrayList<>();

        for (IpamIp ipamIp : result) {
            List<String> tempList = new ArrayList<>();
            tempList.add(ipamIp.getName());
            tempList.add(ipamIp.getIpamSubnet().getName());
            tempList.add(ipamIp.getAddr());
            tempList.add(ipamIp.getMac());
            if (Tool.ObjectUtil.isNotEmpty(ipamIp.getAsset())){
                tempList.add(ipamIp.getAsset().getName());
            }else {
                tempList.add("");
            }
            tempList.add(ipamIp.getState().toString());
            tempList.add(ipamIp.getRemark());
            tempList.add(ipamIp.getUts().toString());
            exportDataList.add(tempList);
        }
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_IPAMIP_EXPORT_HEADER);
        String fileName = "Ipamips";
        String formatStr = params.get("format").toString();
        basicImportAndExportServices.exportDataByFormat(response, exportDataList, sysHeaderJsonStr, formatStr, fileName);
    }

    @Override
    public void cancelImport(String seq) {
        ipamIpDao.delete(new QueryWrapper<IpamIp>().lambda().eq(IpamIp::getSeq, seq));
    }


}
