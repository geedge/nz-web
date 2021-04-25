/*
package com.nis.modules.asset.job;

import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.nis.common.utils.BufferReaderWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.DateUtil;
import com.nis.common.utils.RuntimeUtil;
import com.nis.modules.asset.dao.AssetPingDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.service.AssetPingService;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@DisallowConcurrentExecution
public class AssetPingJob extends QuartzJobBean {

    private static Log logger = Log.get();

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetPingDao assetPingDao;

    @Autowired
    private AssetPingService assetPingService;

    private static Pattern NUMBER_PATTERN = Pattern.compile("[0-9]+(\\.[0-9]+)?");

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 检查 haMode
        */
/*boolean haMode = sysConfigService.checkHaMode();
        if (!haMode) return;

        StopWatch sw = new StopWatch();
        sw.start();
        try {
            // 进行同步
            this.execAssetPing();
        } catch (RuntimeException e) {
            logger.error("asset ping 采集异常",e);
        } finally {
            sw.stop();
        }
        logger.info("asset ping Run Time: {}", sw.toString());*//*

    }

    @Transactional(rollbackFor = Exception.class)
    public void execAssetPing() {
        */
/*List<SysConfigEntity> configs = sysConfigService.list();

        Map<String, String> configMap = new HashMap();
        for (SysConfigEntity configEntity : configs) {
            configMap.put(configEntity.getParamKey(), configEntity.getParamValue());
        }

        // 开启主机连通性开关
        if(StrUtil.equals(Constant.ASSET_PING_OPEN_SWITCH,configMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_SWITCH).trim())) {
            logger.info("asset ping 开关开启");

            // 获取 所有 asset
            List<Asset> assets = assetService.list();

            File file = null;
            if (CollectionUtils.isNotEmpty(assets)) {
                file = this.getIpsFile(assets, file);
            }

            // 文件存在 则证明有状态为 1 的资产Ip
            if (file != null && file.exists()) {

                List<String> resultList = new ArrayList<>();

                StringReader result = null;
                BufferReaderWrapper reader = null;
                try {
                    // 执行命令并返回结果
                    String command = configMap.get(Constant.SYSCONFIG_KEY_ASSET_FPING_COMMAND);

                    String[] commands = command.split(" ");
                    for (int i = 0; i < commands.length; i++) {
                        if(StrUtil.equals(Constant.ASSET_PING_COMMAND_FILENAME,commands[i])) {
                            commands[i] = file.getAbsolutePath();
                        }
                    }

                    StopWatch sw = new StopWatch();
                    sw.start();
                    try {
                        result = RuntimeUtil.run(null, null, commands);
                    } catch (RuntimeException e) {
	                        logger.error(String.format("asset ping 执行命令失败, 执行命令是: %s, 错误信息是: %s", Arrays.asList(commands).toString(), e));
                    } finally {
                        sw.stop();
                    }
                    logger.info("asset ping 执行命令是：" + Arrays.asList(commands).toString());
                    logger.info("asset ping 执行命令时长: " + sw.toString());

                    reader = new BufferReaderWrapper(result);
                    String line;
                    while ((line = reader.load()) != null) {
                        resultList.add(line);
                    }

//                    if (logger.isDebugEnabled()) logger.debug("asset fping 执行结果集 {}", resultList);

                    // ip 和描述信息
                    Map<String, String> ipAndDesc = new HashMap<>();
                    for (String str : resultList) {
                        String[] is = str.split("is");
                        ipAndDesc.put(is[0].trim(), is[1].trim());
                    }

                    // 可达ping asset集合
                    List<AssetPing> callAssetPings = new ArrayList<>();
                    // 不可达ping asset集合
                    List<AssetPing> unCallAssetPings = new ArrayList<>();
                    String statusInfo = null;
                    String host = null;

                    for (Map.Entry<String, String> entry : ipAndDesc.entrySet()) {
                        statusInfo = entry.getValue();
                        host = entry.getKey();
                        // IP 不可达
                        if(StrUtil.equals(Constant.ASSET_PING_DOWN_FLAG,statusInfo)) {
                            unCallAssetPings.add(new AssetPing(host, 0, null, null));
                        } else {
                            // 可达
                            callAssetPings.add(new AssetPing(host, 1, Double.valueOf(getRrt(statusInfo)), new Date()));
                        }
                    }

                    // 更新数据库
                    List<AssetPing> list = assetPingService.list();
                    Map<String, AssetPing> hostAndAssetPing = new HashMap();
                    for (AssetPing assetPing : list) {
                        hostAndAssetPing.put(assetPing.getHost(), assetPing);
                    }
                    // 处理不可达资产的最后可达时间
                    AssetPing ping;
                    for (AssetPing assetPing : unCallAssetPings) {
                        ping = hostAndAssetPing.get(assetPing.getHost());
                        if (ping != null) {
                            assetPing.setLastUpdate(ping.getLastUpdate());
                        }
                    }
                    // 汇总数据
                    callAssetPings.addAll(unCallAssetPings);

                    assetPingDao.deleteAll();
                    assetPingService.saveBatch(callAssetPings);
                } catch (IOException e) {
                    logger.error(e);
                } finally {
                    if (reader != null) {
                        try {
                            reader.close();
                        } catch (IOException e) {
                        }
                    }
                    if (result != null) {
                        result.close();
                    }
                    // 删除临时文件
                    boolean delete = file.delete();
                    if(delete) {
                    	logger.debug("asset ping file deleted");
                    }else {
                    	logger.debug("asset ping file can not be deleted , please check");
                    }
                }
            }
        } else {
            logger.info("asset ping 开关关闭");
        }*//*

    }

    private File getIpsFile(List<Asset> assets, File file) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(file = new File(Constant.TEMP_PATH + File.separator + "assetPingFile_ips" + UUID.randomUUID()));
            bw = new BufferedWriter(fw);
            for (Asset asset : assets) {
                bw.write(asset.getHost());
                bw.newLine();// 换行
                bw.flush();  // 刷流
            }

        } catch (IOException e) {
            logger.error(e);
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
        }
        return file;
    }

    public String getRrt(String str) {
        String returnRrt = null;
        Matcher m = NUMBER_PATTERN.matcher(str);
        if (m.find()) {
            returnRrt = m.group();
        }
        return returnRrt;
    }
}
*/
