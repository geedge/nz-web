package com.nis.modules.ipam.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.StringPool;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.*;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.ipam.dao.IpamIpDao;
import com.nis.modules.ipam.entity.IpamIp;
import com.nis.modules.ipam.entity.IpamSubnet;
import com.nis.modules.ipam.service.IpamIpService;
import com.nis.modules.ipam.service.IpamSubnetService;
import com.nis.modules.sys.service.BasicImportAndExportServices;
import com.nis.modules.sys.service.SysConfigService;
import inet.ipaddr.AddressStringException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

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

    @Override
    public IpamIp ipamIpInfo(Integer id) {

        IpamIp ipamIp = this.getById(id);
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
    @Transactional
    public Integer saveOrModifyIpamSubnet(IpamIp ipamIp) {
        ValidateUtils.is(ipamIp.getName()).notNull(RCode.IPAM_IP_NAME_ISNULL).
                and(ipamIp.getAddr()).notNull(RCode.IPAM_IP_ADDR_ISNULL).
                and(ipamIp.getMac()).notNull(RCode.IPAM_IP_MAC_ISNULL);

        //校验ip
        if (!(CommonUtils.checkIpv4(ipamIp.getAddr()) || CommonUtils.checkIpv6(ipamIp.getAddr()))){
            throw new NZException(RCode.IPAM_IP_ADDR_ERROR);
        }
        //查询subnet
        IpamSubnet ipamSubnet = ipamSubnetService.getOne(new LambdaQueryWrapper<IpamSubnet>().eq(IpamSubnet::getAddr, ipamIp.getAddr()));
        if (Tool.ObjectUtil.isEmpty(ipamSubnet)){
            throw new NZException(RCode.IPAM_IP_SUBNET_ISNULL);
        }
        //校验mac
        if (!CommonUtils.checkMac(ipamIp.getMac())){
            throw new NZException(RCode.IPAM_IP_MAC_ERROR);
        }
        //校验ip mac之前是否创建
        IpamIp ip = this.getOne(new LambdaQueryWrapper<IpamIp>().eq(IpamIp::getAddr, ipamIp.getAddr()).eq(IpamIp::getMac, ipamIp.getMac()));
        if (Tool.ObjectUtil.isNotEmpty(ip)){
            throw new NZException(RCode.IPAM_IP_INTERVAL_ERROR);
        }
        if (Tool.ObjectUtil.isEmpty(ipamIp.getState())){
            ipamIp.setState(1);
        }
        ipamIp.setUts(new Date().getTime());
        ipamIp.setSubnetId(ipamSubnet.getId());
        this.saveOrUpdate(ipamIp);
        return ipamIp.getId();
    }

    @Override
    @Transactional
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
    public Map importIpamIpData(MultipartFile excelFile) throws IOException, AddressStringException {
        String sysHeaderJsonStr = sysConfigService.getValue(Constant.SYSCONFIG_KEY_IPAMIP_IMPORT_HEADER);
        Map<String, String> exportHeaderMap = basicImportAndExportServices.getI18nHeaderMap(sysHeaderJsonStr);
        List<Map<Integer, String>> importDataList = basicImportAndExportServices.getImportDataFromMultipartFile(excelFile, sysHeaderJsonStr);

        List<Map> failDetails = new ArrayList<>();
        String seqUUID = UUID.randomUUID().toString();
        //ArrayList<String> headerInfos = new ArrayList<>(exportHeaderMap.keySet());
        //List<Map<Integer, String>> handleImportData = null;
        int successNum = this.saveIpamIpsFromImportDataList(importDataList, failDetails, new ArrayList<>(exportHeaderMap.keySet()), seqUUID);
        int totalNum = importDataList.size();

        Map resultMap = new HashMap(8);
        resultMap.put("seq", seqUUID);
        resultMap.put("totalNum", totalNum);
        resultMap.put("successNum", successNum);
        resultMap.put("failNum", totalNum - successNum);
        resultMap.put("failDetail", CommonUtils.handleImportFailDetails(failDetails));
        return resultMap;
    }

    private int saveIpamIpsFromImportDataList(List<Map<Integer, String>> importDataList, List<Map> failDetails, ArrayList<String> headerInfos, String seqUUID) throws AddressStringException {
        int successNum = 0;
        for (int i = 0; i < importDataList.size(); i++) {
            int dataFromExcelRowNum = i + 1;
            boolean saveFlag = true;
            Map<Integer, String> dataMap = importDataList.get(i);

            String name = dataMap.get(0);
            if (StringUtils.isEmpty(name)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(0), RCode.IPAM_IP_NAME_ISNULL.getMsg());
            }

            String subnetName = dataMap.get(1);
            IpamSubnet ipamSubnet = null;
            Integer subnetId = null;
            if(StringUtils.isEmpty(subnetName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.IPAM_IP_SUBNET_ISNULL.getMsg());
            }else {
                ipamSubnet = ipamSubnetService.getOne(new QueryWrapper<IpamSubnet>().lambda().eq(IpamSubnet::getName, subnetName));
                if(org.springframework.util.ObjectUtils.isEmpty(ipamSubnet)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(1), RCode.IPAM_IP_SUBNET_ISNULL.getMsg());
                }else {
                    subnetId= ipamSubnet.getId();
                }
            }

            String addr = dataMap.get(2);
            if(StringUtils.isEmpty(addr)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.IPAM_IP_ADDR_ISNULL.getMsg());
            }else {
                if (!(CommonUtils.checkIpv4(addr) || CommonUtils.checkIpv6(addr))){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.IPAM_IP_ADDR_ERROR.getMsg());
                }
                if (!ipamSubnet.getAddr().equals(addr)){
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(2), RCode.IPAM_IP_SUBNET_ADDR_ERROR.getMsg());
                }
            }

            String mac = dataMap.get(3);
            if (StringUtils.isEmpty(mac)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(3), RCode.IPAM_IP_MAC_ISNULL.getMsg());
            }else if(!CommonUtils.checkMac(mac)){
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum,failDetails,headerInfos.get(3),RCode.IPAM_IP_MAC_ERROR.getMsg());
            }

            // 校验ip以及mac值之前是否有创建或者包含
            IpamIp ipamIp1 = this.getOne(new LambdaQueryWrapper<IpamIp>().eq(IpamIp::getAddr, addr).eq(IpamIp::getMac, mac));
            if (Tool.ObjectUtil.isNotEmpty(ipamIp1)){
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum,failDetails,headerInfos.get(3),RCode.IPAM_IP_MAC_ADDR_ERROR.getMsg());
            }

            String assdtName = dataMap.get(4);
            Integer assetId = null;
            if(StringUtils.isEmpty(assdtName)) {
                saveFlag = false;
                this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.IPAM_IP_ASSET_ISNULL.getMsg());
            }else {
                AssetAsset assetAsset = asset.getOne(new QueryWrapper<AssetAsset>().lambda().eq(AssetAsset::getName, assdtName));
                if(org.springframework.util.ObjectUtils.isEmpty(assetAsset)) {
                    saveFlag = false;
                    this.addFailDetail(dataFromExcelRowNum, failDetails, headerInfos.get(4), RCode.IPAM_IP_ASSET_ISNULL.getMsg());
                }else {
                    assetId= assetAsset.getId();
                }
            }

            String remark = dataMap.get(5);

            if (saveFlag) {
                IpamIp ipamIp = new IpamIp();
                try {
                    ipamIp.setName(name);
                    ipamIp.setSubnetId(subnetId);
                    ipamIp.setAddr(addr);
                    ipamIp.setMac(mac);
                    ipamIp.setAssetId(assetId);
                    ipamIp.setRemark(remark);
                    ipamIp.setSeq(seqUUID);
                    ipamIp.setState(1);
                    ipamIp.setUts(new Date().getTime());
                    this.save(ipamIp);
                    successNum++;
                } catch (RuntimeException e) {
                    log.error(String.format("ipamip 导入保存失败,实体信息是：%s,错误信息是：%s", JSONObject.toJSONString(ipamIp), e.getMessage()), e);
                    this.addFailDetail(dataFromExcelRowNum, failDetails, "System", RCode.EXCELFILE_IMPORT_ERROR.getMsg());
                }
            }
        }
        return successNum;
    }
    private void addFailDetail(int i, List<Map> failDetails, String headerInfo, String msg) {
        Map failDetail = new HashMap(4);
        failDetail.put("lineNo", i);
        failDetail.put("errorMsg", headerInfo + ": " + msg);
        failDetails.add(failDetail);
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
