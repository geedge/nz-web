package com.nis.modules.asset.service.impl;

import cn.hutool.core.lang.Opt;
import cn.hutool.core.lang.Validator;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.JSONPath;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jcraft.jsch.Session;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.RCode;
import com.nis.common.utils.SshUtil;
import com.nis.common.utils.Tool;
import com.nis.modules.agent.service.PromApiService;
import com.nis.modules.asset.dao.AssetFeatureDao;
import com.nis.modules.asset.entity.AssetAsset;
import com.nis.modules.asset.entity.AssetFeature;
import com.nis.modules.asset.entity.AssetTalonStatus;
import com.nis.modules.asset.entity.AssetTypeConf;
import com.nis.modules.asset.service.AssetAssetService;
import com.nis.modules.asset.service.AssetFeatureService;
import com.nis.modules.asset.service.AssetTalonStatusService;
import com.nis.modules.asset.service.TalonApiService;
import org.apache.commons.lang3.time.StopWatch;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Th
 */
@Service("assetFeatureService")
public class AssetFeatureServiceImpl extends ServiceImpl<AssetFeatureDao, AssetFeature> implements AssetFeatureService {

    private static final Log log = Log.get();

    @Autowired
    private AssetAssetService assetService;

    @Autowired
    private AssetTypeConfServiceImpl assetTypeConfService;

    @Autowired
    private PromApiService promApiService;

    @Autowired
    private TalonApiService talonApiService;

    @Autowired
    private AssetTalonStatusService assetTalonStatusService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectAssetBasicInfo(Integer assetId) {
        AssetAsset asset = assetService.getById(assetId);
        ValidateUtils.is(asset).notNull(RCode.ASSET_NOT_EXIST);

        // asset_type_conf
        AssetTypeConf typeConf = assetTypeConfService.getById(asset.getTypeId());

        String source = null;
        Map<String, String> assetBasicInfoMap = Tool.MapUtil.empty();

        // source=talon
        Integer talonEnable = asset.getClientEnable();
        AssetTalonStatus assetTalonStatus = assetTalonStatusService.getOne(new LambdaQueryWrapper<AssetTalonStatus>().eq(AssetTalonStatus::getAssetId, assetId));
        if (Tool.BooleanUtil.and(
                Tool.ObjectUtil.equal(1, talonEnable),
                Tool.ObjectUtil.equal(1, assetTalonStatus.getStatus()))
        ) {
            if (log.isDebugEnabled()) {
                log.debug("[collectAssetBasicInfo] [source=TALON] [asset: {}]", assetId);
            }
            source = "TALON";
            assetBasicInfoMap = this.collectAssetInfoWithTalon(assetId);
        }

        // source=node_exporter
        Integer nodeCollect = typeConf.getNodeCollect();
        if (Tool.BooleanUtil.and(
                assetBasicInfoMap.isEmpty(),
                Tool.ObjectUtil.equal(1, nodeCollect))
        ) {
            if (log.isDebugEnabled()) {
                log.debug("[collectAssetBasicInfo] [source=NODE] [asset: {}]", assetId);
            }
            source = "NODE";
            assetBasicInfoMap = this.collectAssetInfoWithNode(assetId);
        }

        // source=ssh
        Integer sshCollect = typeConf.getSshCollect();
        Integer authProtocol = typeConf.getAuthProtocol();
        if (Tool.BooleanUtil.and(
                assetBasicInfoMap.isEmpty(),
                Tool.ObjectUtil.equal(1, sshCollect),
                Tool.ObjectUtil.equal(1, authProtocol))
        ) {
            if (log.isDebugEnabled()) {
                log.debug("[collectAssetBasicInfo] [source=SSH] [asset: {}]", assetId);
            }
            source = "SSH";
            assetBasicInfoMap = this.collectAssetInfoWithSSH(assetId, typeConf.getSshCollectScript());
        }

        // update
        if (Tool.MapUtil.isNotEmpty(assetBasicInfoMap)) {
            this.remove(new LambdaQueryWrapper<AssetFeature>().eq(AssetFeature::getAssetId, assetId));
            List<AssetFeature> list = Tool.ListUtil.list(true);
            for (Map.Entry<String, String> entry : assetBasicInfoMap.entrySet()) {
                AssetFeature feature = new AssetFeature();
                feature.setAssetId(assetId);
                feature.setItem(entry.getKey());
                feature.setValue(entry.getValue());
                feature.setSource(source);
                feature.setUpdateAt(new Date());
                list.add(feature);
            }
            if (Tool.CollUtil.isNotEmpty(list)) {
                this.saveBatch(list);
                log.info("[collectAssetBasicInfo] [finshed] [asset: {}] [source: {}] [size: {}]", assetId, source, list.size());
            }
        } else {
            log.warn("[collectAssetBasicInfo] [no data collect] [asset: {}]", assetId);
        }
    }

    /**
     * collect asset info with talon
     *
     * @param assetId
     * @return
     */
    private Map<String, String> collectAssetInfoWithTalon(Integer assetId) {
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            JSONObject oshiInfo = talonApiService.oshiInfo(assetId);
            oshiInfo = Tool.ObjectUtil.defaultIfNull(oshiInfo, new JSONObject());

            Object code = Opt.ofNullable(oshiInfo).map(jsonObject -> jsonObject.get("code")).get();
            if (log.isDebugEnabled()) {
                log.debug("[collectAssetInfoWithTalon] [oshi info of talon api response] [asset: {}] [code: {}]", assetId, code);
            }
            if (ObjectUtil.equal(RCode.SUCCESS.getCode(), code)) {
                JSONObject osInfo = (JSONObject) JSONPath.eval(oshiInfo, "data.os");
                JSONObject cpuInfo = (JSONObject) JSONPath.eval(oshiInfo, "data.cpu");
                JSONObject memoryInfo = (JSONObject) JSONPath.eval(oshiInfo, "data.memory");
                JSONArray diskInfoList = (JSONArray) JSONPath.eval(oshiInfo, "data.disk");
                JSONArray networkIFInfoList = (JSONArray) JSONPath.eval(oshiInfo, "data.networkIF");

                Map<String, String> assetBasicInfoMap = Tool.MapUtil.builder("os", JSONObject.toJSONString(osInfo))
                        .put("cpu", JSONObject.toJSONString(cpuInfo))
                        .put("memory", JSONObject.toJSONString(memoryInfo))
                        .put("disk", JSONObject.toJSONString(diskInfoList))
                        .put("networkIF", JSONObject.toJSONString(networkIFInfoList))
                        .build();
                return assetBasicInfoMap;
            }
        } catch (Exception e) {
            log.error(e, "[collectAssetInfoWithTalon] [error] [assetId: {}]", assetId);
        } finally {
            sw.stop();
        }
        if (log.isDebugEnabled()) {
            log.debug("[collectAssetInfoWithTalon] [finshed] [assetId: {}] [Run Time: {}]", assetId, sw.toString());
        }
        return Tool.MapUtil.empty();
    }


    /**
     * collect asset info with node_exporter
     *
     * @param assetId
     * @return
     */
    public Map<String, String> collectAssetInfoWithNode(Integer assetId) {
        StopWatch sw = new StopWatch();
        sw.start();
        try {
            Map<String, Object> osInfoMap = this.buildOsInfoWithNode(assetId);
            Map<String, Object> cpuInfoMap = this.buildCpuInfoWithNode(assetId);
            Map<String, Object> memoryInfoMap = this.buildMemoryInfoWithNode(assetId);
            List<Map<String, Object>> diskInfoList = this.buildDiskInfoWithNode(assetId);
            List<Map<String, Object>> networkIFInfoList = this.buildNetworkIFInfoWithNode(assetId);

            Map<String, String> assetBasicInfoMap = Tool.MapUtil.builder("os", JSONObject.toJSONString(osInfoMap))
                    .put("cpu", JSONObject.toJSONString(cpuInfoMap))
                    .put("memory", JSONObject.toJSONString(memoryInfoMap))
                    .put("disk", JSONObject.toJSONString(diskInfoList))
                    .put("networkIF", JSONObject.toJSONString(networkIFInfoList))
                    .build();
            return assetBasicInfoMap;
        } catch (Exception e) {
            log.error(e, "[collectAssetInfoWithNode] [error] [assetId: {}]", assetId);
        } finally {
            sw.stop();
        }
        if (log.isDebugEnabled()) {
            log.debug("[collectAssetInfoWithNode] [finshed] [assetId: {}] [Run Time: {}]", assetId, sw.toString());
        }
        return Tool.MapUtil.empty();
    }

    /**
     * collect asset info with ssh
     *
     * @param assetId
     * @param sshCollectScript
     * @return
     */
    public Map<String, String> collectAssetInfoWithSSH(Integer assetId, String sshCollectScript) {
        StopWatch sw = new StopWatch();
        sw.start();
        Session session = null;
        try {
            AssetAsset asset = assetService.getBaseMapper().selectById(assetId);
            session = SshUtil.getSshConnection(asset);

            if (StrUtil.isNotEmpty(sshCollectScript)) {
                String cmd = """
                        cat > /tmp/device_info_collector.sh  << "EOF" 
                                          
                        """;
                cmd += sshCollectScript;
                String cmdEnd = """
                                                
                        EOF
                        """;
                cmd += cmdEnd;
                // 写入
                Tool.JschUtil.exec(session, cmd, StandardCharsets.UTF_8);
                // 执行
                String result = Tool.JschUtil.exec(session,
                        "chmod +x /tmp/device_info_collector.sh && /tmp/device_info_collector.sh",
                        StandardCharsets.UTF_8);
                result = StrUtil.nullToDefault(result, "");
                log.info("[collectAssetInfoWithSSH] [script execution result] [size: {}]", result.length());
                if (StrUtil.isNotEmpty(result)) {
                    JSONObject jsonObject = JSONObject.parseObject(result);
                    JSONObject osInfo = (JSONObject) JSONPath.eval(jsonObject, "os");
                    JSONObject cpuInfo = (JSONObject) JSONPath.eval(jsonObject, "cpu");
                    JSONObject memoryInfo = (JSONObject) JSONPath.eval(jsonObject, "memory");
                    JSONArray diskInfoList = (JSONArray) JSONPath.eval(jsonObject, "disk");
                    JSONArray networkIFInfoList = (JSONArray) JSONPath.eval(jsonObject, "networkIF");

                    Map<String, String> assetBasicInfoMap = Tool.MapUtil.builder("os", JSONObject.toJSONString(osInfo))
                            .put("cpu", JSONObject.toJSONString(cpuInfo))
                            .put("memory", JSONObject.toJSONString(memoryInfo))
                            .put("disk", JSONObject.toJSONString(diskInfoList))
                            .put("networkIF", JSONObject.toJSONString(networkIFInfoList))
                            .build();
                    return assetBasicInfoMap;
                }
            } else {
                log.warn("[collectAssetInfoWithSSH] [ssh_collect_script is empty] [asset: {}]", assetId);
            }
            return Tool.MapUtil.empty();
        } catch (Exception e) {
            log.error(e, "[collectAssetInfoWithSSH] [error] [asset: {}]", assetId);
        } finally {
            SshUtil.close(session);
            sw.stop();
        }
        if (log.isDebugEnabled()) {
            log.debug("[collectAssetInfoWithSSH] [finshed] [assetId: {}] [Run Time: {}]", assetId, sw.toString());
        }
        return Tool.MapUtil.empty();
    }

    /**
     * build os info with node_exporter
     *
     * @param assetId
     * @return
     */
    private Map<String, Object> buildOsInfoWithNode(Integer assetId) {
        try {
            JSONObject nodeOsInfo = promApiService.query("node_os_info{asset_id='" + assetId + "'}", null, null);
            JSONObject nodeUnameInfo = promApiService.query("node_uname_info{asset_id='" + assetId + "'}", null, null);
            JSONObject nodeNetworkRouteInfo = promApiService.query("node_network_route_info{dest='default',asset_id='" + assetId + "'}", null, null);

            Map<String, Object> os = Tool.MapUtil.newHashMap();

            JSONObject unameMetric = (JSONObject) JSONPath.eval(nodeUnameInfo, "data.result[0].metric");
            if (ObjectUtil.isNotEmpty(unameMetric)) {
                String machine = unameMetric.getString("machine");
                os.put("bitness", machine.contains("64") ? 64 : 32);
                os.put("hostName", unameMetric.getString("nodename"));
                os.put("versionInfo", unameMetric.getString("release"));
                os.put("platform", unameMetric.getString("sysname"));
                os.put("manufacturer", unameMetric.getString("sysname"));
            }

            JSONObject osInfoMetric = (JSONObject) JSONPath.eval(nodeOsInfo, "data.result[0].metric");
            if (ObjectUtil.isNotEmpty(osInfoMetric)) {
                String versionInfo = (String) os.getOrDefault("versionInfo", StrUtil.EMPTY);
                String osPrettyName = osInfoMetric.getString("pretty_name");
                os.put("versionInfo", StrUtil.concat(true, osPrettyName, " ", versionInfo));
                os.put("family", osInfoMetric.getString("name"));
            }

            JSONArray networkRouteMetricList = (JSONArray) JSONPath.eval(nodeNetworkRouteInfo, "data.result.metric");
            if (ObjectUtil.isNotEmpty(networkRouteMetricList)) {
                for (Object object : networkRouteMetricList) {
                    JSONObject networkRouteMetric = (JSONObject) object;
                    String gw = networkRouteMetric.getString("gw");
                    if (Validator.isIpv4(gw)) {
                        os.putIfAbsent("ipv4DefaultGateway", gw);
                    } else if (Validator.isIpv6(gw)) {
                        os.putIfAbsent("ipv6DefaultGateway", gw);
                    }
                }
            }
            return os;
        } catch (Exception e) {
            log.error(e, "[buildOsInfoWithNode] [error] [assetId: {}]", assetId);
        }
        return Tool.MapUtil.empty();
    }

    /**
     * build cpu info with node_exporter
     *
     * @param assetId
     * @return
     */
    private Map<String, Object> buildCpuInfoWithNode(Integer assetId) {
        try {
            JSONObject nodeCpuInfo = promApiService.query("node_cpu_info{asset_id='" + assetId + "'}", null, null);
            JSONObject nodeIntrTotal = promApiService.query("node_intr_total{asset_id='" + assetId + "'}", null, null);
            JSONObject nodeCpuFlagInfo = promApiService.query("node_cpu_flag_info{flag='lm',asset_id='" + assetId + "'}", null, null);
            JSONObject nodeContextSwitchesTotal = promApiService.query("node_context_switches_total{asset_id='" + assetId + "'}", null, null);

            Map<String, Object> cpu = Tool.MapUtil.newHashMap();
            // 默认值
            cpu.put("cpu64bit", false);
            JSONObject cpuLmFlag = (JSONObject) JSONPath.eval(nodeCpuFlagInfo, "data.result[0].metric");
            if (ObjectUtil.isNotEmpty(cpuLmFlag)) {
                cpu.put("cpu64bit", true);
            }

            Object contextSwitchesTotalValue = JSONPath.eval(nodeContextSwitchesTotal, "data.result[0].value[1]");
            cpu.put("contextSwitches", contextSwitchesTotalValue);

            Object intrTotalValue = JSONPath.eval(nodeIntrTotal, "data.result[0].value[1]");
            cpu.put("interrupts", intrTotalValue);

            JSONArray cpuInfoList = (JSONArray) JSONPath.eval(nodeCpuInfo, "data.result.metric");
            if (ObjectUtil.isNotEmpty(cpuInfoList)) {
                JSONObject firstObj = (JSONObject) cpuInfoList.getFirst();

                String family = firstObj.getString("family");
                String model = firstObj.getString("model");
                String stepping = firstObj.getString("stepping");
                String vendor = firstObj.getString("vendor");

                cpu.putIfAbsent("family", family);
                cpu.putIfAbsent("model", model);
                cpu.putIfAbsent("stepping", stepping);
                cpu.putIfAbsent("vendor", vendor);

                cpu.putIfAbsent("name", firstObj.getString("model_name"));
                // 获取 microcode
                cpu.putIfAbsent("microarchitecture", firstObj.getString("microcode"));

                Boolean cpu64bit = Tool.MapUtil.getBool(cpu, "cpu64bit");
                String identifier = StrUtil.concat(true, vendor, cpu64bit ? "64" : "32", " Family ", family, " Model ", model, " Stepping ", stepping);
                cpu.putIfAbsent("identifier", identifier);


                long physicalPackageCount = cpuInfoList.stream().map(obj -> ((JSONObject) obj).getString("package").toString()).distinct().count();
                cpu.putIfAbsent("physicalPackageCount", physicalPackageCount);

                long logicalProcessorCount = cpuInfoList.stream().map(obj -> ((JSONObject) obj).getString("cpu").toString()).distinct().count();
                cpu.putIfAbsent("logicalProcessorCount", logicalProcessorCount);
                cpu.putIfAbsent("physicalProcessorCount", logicalProcessorCount / physicalPackageCount);
            }
            return cpu;
        } catch (Exception e) {
            log.error(e, "[buildCpuInfoWithNode] [error] [assetId: {}]", assetId);
        }
        return Tool.MapUtil.empty();
    }

    /**
     * build memory info with node_exporter
     *
     * @param assetId
     * @return
     */
    private Map<String, Object> buildMemoryInfoWithNode(Integer assetId) {
        try {
            JSONObject nodeMemoryMemTotalBytes = promApiService.query("node_memory_MemTotal_bytes{asset_id='" + assetId + "'}", null, null);
            JSONObject nodeMemorySwapTotalBytes = promApiService.query("node_memory_SwapTotal_bytes{asset_id='" + assetId + "'}", null, null);
            JSONObject nodeMemoryCommitLimitBytes = promApiService.query("node_memory_CommitLimit_bytes{asset_id='" + assetId + "'}", null, null);

            Map<String, Object> memory = Tool.MapUtil.newHashMap();
            Object memoryMemTotalBytesValue = JSONPath.eval(nodeMemoryMemTotalBytes, "data.result[0].value[1]");
            memory.put("total", memoryMemTotalBytesValue);

            Object memorySwapTotalBytesValue = JSONPath.eval(nodeMemorySwapTotalBytes, "data.result[0].value[1]");
            memory.put("swapTotal", memorySwapTotalBytesValue);

            Object memoryCommitLimitBytesValue = JSONPath.eval(nodeMemoryCommitLimitBytes, "data.result[0].value[1]");
            memory.put("virtualMax", memoryCommitLimitBytesValue);
            return memory;
        } catch (Exception e) {
            log.error(e, "[buildMemoryInfoWithNode] [error] [assetId: {}]", assetId);
        }
        return Tool.MapUtil.empty();
    }

    /**
     * build disk info with node_exporter
     *
     * @param assetId
     * @return
     */
    private List<Map<String, Object>> buildDiskInfoWithNode(Integer assetId) {
        try {
            JSONObject nodeDiskInfo = promApiService.query("node_disk_info{asset_id='" + assetId + "'}", null, null);

            List<Map<String, Object>> disk = Tool.ListUtil.list(true);
            JSONArray diskInfoList = (JSONArray) JSONPath.eval(nodeDiskInfo, "data.result.metric");
            if (ObjectUtil.isNotEmpty(diskInfoList)) {
                for (Object object : diskInfoList) {
                    JSONObject jsonObject = (JSONObject) object;
                    Map<String, Object> tmap = Tool.MapUtil.newHashMap();
                    String device = jsonObject.getString("device");
                    if (!StrUtil.startWith(device, "sd")) {
                        continue;
                    }

                    String deviceFullName = StrUtil.concat(true, "/dev/", device);
                    tmap.put("name", deviceFullName);

                    String modelName = jsonObject.getString("model");
                    if (StrUtil.isNotEmpty(modelName)) {
                        tmap.put("model", modelName);

                        String serial = jsonObject.getString("serial");
                        tmap.put("serial", StrUtil.emptyToDefault(serial, "unknown"));
                    } else {
                        tmap.put("model", "Logical Volume Group");

                        JSONObject nodeDiskDeviceMapperInfo = promApiService.query("node_disk_device_mapper_info{device='" + device + "',asset_id='" + assetId + "'}", null, null);
                        Object serialUid = JSONPath.eval(nodeDiskDeviceMapperInfo, "data.result[0].metric.uuid");
                        tmap.put("serial", ObjectUtil.isNotEmpty(serialUid) ? serialUid : "unknown");
                    }


                    // disk size 通过正则匹配查询
                    String searchName = deviceFullName.replaceAll("\\d*$", "");
                    // sum(node_filesystem_size_bytes{device=~'/dev/sda.*'}) 计算。包含 /dev/sda1,/dev/sd2,/dev/sd3,...
                    JSONObject nodeFilesystemSizeBytes = promApiService.query("sum by (endpoint) (node_filesystem_size_bytes{device=~'" + searchName + ".*',asset_id='" + assetId + "'})", null, null);

                    Object filesystemSizeBytesValue = JSONPath.eval(nodeFilesystemSizeBytes, "data.result[0].value[1]");
                    tmap.put("size", filesystemSizeBytesValue);

                    disk.add(tmap);
                }
            }
            disk = disk.stream().distinct().collect(Collectors.toList());
            return disk;
        } catch (Exception e) {
            log.error(e, "[buildDiskInfoWithNode] [error] [assetId: {}]", assetId);
        }
        return Tool.ListUtil.empty();
    }

    /**
     * build networkIF info with node_exporter
     *
     * @param assetId
     * @return
     */
    private List<Map<String, Object>> buildNetworkIFInfoWithNode(Integer assetId) {
        try {
            JSONObject nodeNetworkAddressInfo = promApiService.query("node_network_address_info{asset_id='" + assetId + "'}", null, null);

            List<Map<String, Object>> networkIF = Tool.ListUtil.list(true);
            JSONArray networkIFInfoList = (JSONArray) JSONPath.eval(nodeNetworkAddressInfo, "data.result.metric");
            if (ObjectUtil.isNotEmpty(networkIFInfoList)) {
                // 根据 device 标签进行分组
                Map<String, List<Object>> listMap = networkIFInfoList.stream().collect(Collectors.groupingBy(object -> ((JSONObject) object).getString("device")));
                for (Map.Entry<String, List<Object>> entry : listMap.entrySet()) {
                    Map<String, Object> tmap = Tool.MapUtil.newHashMap();

                    String deviceName = entry.getKey();
                    tmap.put("name", deviceName);

                    Set<String> iPv4addrList = Tool.CollUtil.set(true);
                    Set<String> iPv6addrList = Tool.CollUtil.set(true);
                    // The Internet Protocol (IP) v4 subnet masks.
                    Set<String> subnetMasksList = Tool.CollUtil.set(true);
                    for (Object obj : entry.getValue()) {
                        JSONObject jsonObject = (JSONObject) obj;
                        String address = jsonObject.getString("address");
                        if (Validator.isIpv4(address)) {
                            iPv4addrList.add(address);
                            subnetMasksList.add(jsonObject.getString("netmask"));
                        } else if (Validator.isIpv6(address)) {
                            iPv6addrList.add(address);
                        }
                    }
                    tmap.put("iPv4addr", iPv4addrList);
                    tmap.put("iPv6addr", iPv6addrList);
                    tmap.put("subnetMasks", subnetMasksList);

                    JSONObject nodeNetworkMtuBytes = promApiService.query("node_network_mtu_bytes{device='" + deviceName + "',asset_id='" + assetId + "'}", null, null);
                    Object networkMtuBytesValue = JSONPath.eval(nodeNetworkMtuBytes, "data.result[0].value[1]");
                    tmap.put("mtu", networkMtuBytesValue);

                    JSONObject nodeNetworkInfo = promApiService.query("node_network_info{device='" + deviceName + "',asset_id='" + assetId + "'}", null, null);
                    JSONObject networkInfoMetric = (JSONObject) JSONPath.eval(nodeNetworkInfo, "data.result[0].metric");
                    if (ObjectUtil.isNotEmpty(networkInfoMetric)) {
                        String address = networkInfoMetric.getString("address");
                        tmap.put("macaddr", address);

                        // link oshi.hardware.NetworkIF.IfOperStatus
                        String operstate = networkInfoMetric.getString("operstate");
                        tmap.put("ifOperStatus", StrUtil.equalsIgnoreCase("up", operstate) ? 1 : 2);

                        String ifalias = networkInfoMetric.getString("ifalias");
                        tmap.put("ifAlias", StrUtil.emptyToDefault(ifalias, ""));
                    }

                    JSONObject nodeNetworkIfaceId = promApiService.query("node_network_iface_id{device='" + deviceName + "',asset_id='" + assetId + "'}", null, null);
                    Object networkIfaceIdValue = JSONPath.eval(nodeNetworkIfaceId, "data.result[0].value[1]");
                    tmap.put("index", networkIfaceIdValue);

                    JSONObject nodeNetworkProtocolType = promApiService.query("node_network_protocol_type{device='" + deviceName + "',asset_id='" + assetId + "'}", null, null);
                    Object networkProtocolTypeValue = JSONPath.eval(nodeNetworkProtocolType, "data.result[0].value[1]");
                    tmap.put("ifType", networkProtocolTypeValue);

                    JSONObject nodeNetworkSpeedBytes = promApiService.query("node_network_speed_bytes{device='" + deviceName + "',asset_id='" + assetId + "'}", null, null);
                    Object networkSpeedBytesValue = JSONPath.eval(nodeNetworkSpeedBytes, "data.result[0].value[1]");
                    tmap.put("speed", ObjectUtil.isNotNull(networkSpeedBytesValue) ? Long.valueOf(networkSpeedBytesValue.toString()) * 8 : 0);

                    networkIF.add(tmap);
                }
            }
            networkIF = networkIF.stream().distinct().collect(Collectors.toList());
            return networkIF;
        } catch (Exception e) {
            log.error(e, "[buildNetworkIFInfoWithNode] [error] [assetId: {}]", assetId);
        }
        return Tool.ListUtil.empty();
    }
}
