package com.nis.modules.asset.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.nis.common.utils.Constant;
import com.nis.common.utils.RuntimeUtil;
import com.nis.modules.asset.dao.AssetPingDao;
import com.nis.modules.asset.entity.Asset;
import com.nis.modules.asset.entity.AssetPing;
import com.nis.modules.asset.service.AssetPingService;
import com.nis.modules.asset.service.AssetService;
import com.nis.modules.romote.util.StringUtils;
import com.nis.modules.sys.entity.SysConfigEntity;
import com.nis.modules.sys.service.SysConfigService;
import org.apache.commons.collections.CollectionUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AssetPingJob extends QuartzJobBean {

    private static Logger logger = LoggerFactory.getLogger(AssetPingJob.class);

    @Autowired
    private SysConfigService sysConfigService;

    @Autowired
    private AssetService assetService;

    @Autowired
    private AssetPingDao assetPingDao;

    @Autowired
    private AssetPingService assetPingService;

    private static Pattern NUMBER_PATTERN = Pattern.compile("(\\d+\\.\\d+)");

    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        long start, end;
        start = System.currentTimeMillis();

        // 进行同步
        this.execAssetPing();
        end = System.currentTimeMillis();

        logger.info("asset ping job start time:" + start + "; end time:" + end + "; Run Time:" + (end - start) + "(ms)");
    }

    @Transactional(rollbackFor = Exception.class)
    public void execAssetPing() {
        List<SysConfigEntity> configs = sysConfigService.list();

        Map<String, String> configMap = new HashMap();
        for (SysConfigEntity configEntity : configs) {
            configMap.put(configEntity.getParamKey(), configEntity.getParamValue());
        }

        // 开启主机连通性开关
        if (Constant.ASSET_PING_OPEN_SWITCH.equals(Integer.valueOf(configMap.get(Constant.SYSCONFIG_KEY_ASSET_PING_SWITCH).trim()))) {
            logger.info("asset ping switch is open");

            // 获取 asset state 为 1 的IP信息
            List<Asset> assets = assetService.list(new QueryWrapper<Asset>().eq("state", 1));

            File file = null;
            if (CollectionUtils.isNotEmpty(assets)) {
                file = this.getIpsFile(assets, file);
            }

            // 文件存在 则证明有状态为 1 的资产Ip
            if (file != null && file.exists()) {

                List<String> resultList = new ArrayList<>();

                StringReader result = null;
                BufferedReader reader = null;
                try {
                    // 执行命令并返回结果
                    String command = configMap.get(Constant.SYSCONFIG_KEY_ASSET_FPING_COMMAND);
                    if (StringUtils.isEmpty(command)) {
                        logger.info("asset fping command error");
                    }

                    String[] commands = command.split(" ");
                    for (int i = 0; i < commands.length; i++) {
                        if (Constant.ASSET_PING_COMMAND_FILENAME.equals(commands[i])) {
                            commands[i] = file.getAbsolutePath();
                        }
                    }

                    long start, end;
                    start = System.currentTimeMillis();

                    result = RuntimeUtil.run(null, null, commands);
                    end = System.currentTimeMillis();

                    logger.info("Fping command execution start time:" + start + ";request end time:" + end + "; Run Time:" + (end - start) + "(ms)");

                    reader = new BufferedReader(result);
                    String line;
                    while ((line = reader.readLine()) != null) {
                        resultList.add(line);
                    }

                    logger.debug("asset fping result list {}", resultList);

                    // ip 和描述信息
                    Map<String, String> ipAndDesc = new HashMap<>();
                    for (String str : resultList) {
                        String[] is = str.split("is");
                        ipAndDesc.put(is[0].trim(), is[1].trim());
                    }

                    // 最终更新数据库数据
                    List<AssetPing> assetPings = new ArrayList<>();
                    String statusInfo = null;
                    String host = null;

                    for (Map.Entry<String, String> entry : ipAndDesc.entrySet()) {
                        statusInfo = entry.getValue();
                        host = entry.getKey();
                        // IP 不可达
                        if (Constant.ASSET_PING_DOWN_FLAG.equals(statusInfo)) {
                            assetPings.add(new AssetPing(host, 0, null, new Date()));
                        } else {
                            // 可达
                            assetPings.add(new AssetPing(host, 1, Double.valueOf(getRrt(statusInfo)), new Date()));
                        }
                    }

                    // 更新数据库
                    assetPingDao.deleteAll();
                    assetPingService.saveBatch(assetPings);

                    logger.debug("update asset ping list {}", assetPings);

                } catch (IOException e) {
                    e.printStackTrace();
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
                    file.delete();
                }
            }
        } else {
            logger.info("asset ping switch is down");
        }
    }

    private File getIpsFile(List<Asset> assets, File file) {
        FileWriter fw = null;
        BufferedWriter bw = null;
        try {
            fw = new FileWriter(file = new File("assetPingFile_ips" + UUID.randomUUID()));
            bw = new BufferedWriter(fw);
            for (Asset asset : assets) {
                bw.write(asset.getHost());
                bw.newLine();// 换行
                bw.flush();  // 刷流
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bw != null) {
                try {
                    bw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return file;
    }

    public String getRrt(String str) {
        String returnRrt = null;
        Matcher m = NUMBER_PATTERN.matcher(str);
        if (m.find()) {
            returnRrt = m.group(1);
        }
        return returnRrt;
    }

}
