package com.nis.modules.terminal.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.log.Log;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONPath;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.common.exception.NZException;
import com.nis.common.smartvalidate.ValidateUtils;
import com.nis.common.utils.JSONUtil;
import com.nis.common.utils.RCode;
import com.nis.common.utils.StringUtils;
import com.nis.common.utils.Tool;
import com.nis.modules.sys.service.SysConfService;
import com.nis.modules.terminal.dao.TerminalRecordDao;
import com.nis.modules.terminal.entity.TerminalRecord;
import com.nis.modules.terminal.service.TerminalRecordService;
import com.nis.modules.terminal.utils.TerminalConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("terminalRecordService")
public class TerminalRecordServiceImpl extends ServiceImpl<TerminalRecordDao, TerminalRecord> implements TerminalRecordService {

    private static final Log log = Log.get();

    @Autowired
    private SysConfService sysConfService;

    @Override
    public Map queryTerminalRecordInfos(Map<String, Object> params) {
        // uuid 不能为空
        ValidateUtils.is(params.get("uuid")).notNull(RCode.TERMINAL_UUID_ISNULL);

        Integer time = (params.get("time") != null && StringUtils.isNotEmpty(params.get("time").toString())) ? Integer.valueOf(params.get("time").toString()) : 0;
        Integer size = (params.get("size") != null && StringUtils.isNotEmpty(params.get("size").toString())) ? Integer.valueOf(params.get("size").toString()) : 1;

        if (size < 1) {
            throw new NZException(RCode.TERMINAL_QUERYSIZE_INVALIDED);
        }

        Map<Object, Object> resultMap = Tool.MapUtil.builder().map();
        resultMap.put("uuid", params.get("uuid"));
        resultMap.put("startTime", time);
        resultMap.put("endTime", null);
        resultMap.put("hasNext", 0);
        resultMap.put("list", Collections.emptyList());

        // content Str List of uuid
        List<String> contentStrListOfUuid = Tool.ListUtil.list(true);

        // record List In Db
        List<TerminalRecord> recordListInDb = this.list(new LambdaQueryWrapper<TerminalRecord>()
                .eq(TerminalRecord::getUuid, params.get("uuid"))
                .orderByAsc(TerminalRecord::getStartTime));

        log.info("[queryTerminalRecordInfos] [record data in db] [uuid: {}] [size: {}]", params.get("uuid"), recordListInDb.size());

        if (Tool.CollUtil.isNotEmpty(recordListInDb)) {
            // storage type
            Integer recordStorageType = recordListInDb.stream().findFirst().get().getStorageType();
            if (ObjectUtil.equals(1, recordStorageType)) {
                log.info("[queryTerminalRecordInfos] [record data source is db] [uuid: {}] [size: {}]", params.get("uuid"), recordListInDb.size());
                // db 本地存储
                contentStrListOfUuid = recordListInDb.stream().map(record -> StrUtil.str(record.getContent(), CharsetUtil.charset("UTF-8"))).collect(Collectors.toList());
            } else if (ObjectUtil.equals(2, recordStorageType)) {
                log.info("[queryTerminalRecordInfos] [record data source is s3] [uuid: {}] [size in db: {}]", params.get("uuid"), recordListInDb.size());
                // S3 对象存储
                AmazonS3 s3Client = this.buildTerminalStorageS3Client();
                try {
                    String bucketName = sysConfService.getValue(TerminalConstant.TERMINAL_STORAGE_S3_BUCKET);

                    /*
                    // query objects in s3
                    ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
                    listObjectsV2Request
                            .withBucketName(bucketName)
                            .withMaxKeys(1000)
                            .withPrefix(StrUtil.toString(params.get("uuid")));

                    ListObjectsV2Result listObjectsV2Result = s3Client.listObjectsV2(listObjectsV2Request);
                    List<S3ObjectSummary> S3ObjectSummarylist = listObjectsV2Result.getObjectSummaries();
                    // 再次排序
                    List<String> fileNameList = S3ObjectSummarylist.stream().map(S3ObjectSummary::getKey)
                            .sorted(Comparator.comparing(str -> Integer.valueOf(Tool.StrUtil.subAfter(str, "/", true)))).collect(Collectors.toList());*/

                    List<String> fileNameList = recordListInDb.stream().map(TerminalRecord::getPath)
                            .sorted(Comparator.comparing(str -> Integer.valueOf(Tool.StrUtil.subAfter(str, "/", true)))).collect(Collectors.toList());
                    for (String fileName : fileNameList) {
                        String objectAsString = s3Client.getObjectAsString(bucketName, fileName);
                        contentStrListOfUuid.add(objectAsString);
                    }
                    log.info("[queryTerminalRecordInfos] [record data source is s3] [uuid: {}] [size in db: {}] [size in s3: {}]", params.get("uuid"), fileNameList.size(), contentStrListOfUuid.size());
                } finally {
                    s3Client.shutdown();
                }
            }
        }

        if (Tool.CollUtil.isNotEmpty(contentStrListOfUuid)) {
            // get List Of Content Obj Older Than Time Param
            List<Map> contentObjList = this.getListOfContentObjOlderThanTimeParam(contentStrListOfUuid, time);

            // size 为 1 表示不限制条数
            if (BooleanUtil.and(ObjectUtil.notEqual(size, 1),
                    contentObjList.size() > size)) {
                contentObjList = contentObjList.subList(0, size);
                resultMap.put("hasNext", 1);
            }

            Map endEntity = contentObjList.get(contentObjList.size() - 1);
            resultMap.put("endTime", endEntity.get("t"));
            resultMap.put("list", contentObjList);
        }
        return resultMap;
    }

    /**
     * get List Of Content Obj Older Than Time Param
     *
     * @param contentList
     * @param time
     * @return
     */
    private List<Map> getListOfContentObjOlderThanTimeParam(List<String> contentList, Integer time) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");

        for (String content : contentList) {
            builder.append(content);
        }

        String str = builder.substring(0, builder.length() - 1);

        List<Map> maps = JSONArray.parseArray(str += "]", Map.class);
        maps = maps.stream().filter(tempEntity -> {
            Integer t = (Integer) tempEntity.get("t");
            if (t > time) {
                return true;
            }
            return false;
        }).collect(Collectors.toList());

        return maps;
    }

    @Override
    public void saveTerminalRecordContent(TerminalRecord record) {
        // 存储模式，1: 本地存储，2：S3对象存储
        String terminalStorageType = sysConfService.getValue(TerminalConstant.TERMINAL_STORAGE_TYPE);
        terminalStorageType = StrUtil.emptyToDefault(terminalStorageType, "1");
        log.info("[saveTerminalRecordContent] [storage type] [value: {}] [uuid: {}]", terminalStorageType, record.getUuid());

        // storageType
        record.setStorageType(Integer.valueOf(terminalStorageType));

        switch (terminalStorageType) {
            case "1": {
                // 本地存储
                this.save(record);
                log.info("[saveTerminalRecordContent] [local storage record succeeded] [uuid: {}]", record.getUuid());
                break;
            }
            case "2": {
                // 保存记录至 db，不包含 content 内容
                TerminalRecord saveRecordEntity = new TerminalRecord();
                BeanUtil.copyProperties(record, saveRecordEntity, "content");


                String uuid = record.getUuid();
                Integer startTime = record.getStartTime();

                // 以 uuid 为文件路径，startTime 为文件名称
                String key = StrUtil.concat(true, uuid, "/", startTime.toString());

                saveRecordEntity.setPath(key);
                this.save(saveRecordEntity);

                // S3 对象存储
                AmazonS3 s3Client = this.buildTerminalStorageS3Client();

                String bucketName = sysConfService.getValue(TerminalConstant.TERMINAL_STORAGE_S3_BUCKET);
                boolean doesBucketExist = s3Client.doesBucketExistV2(bucketName);
                log.info("[saveTerminalRecordContent] [bucketName: {}] [doesBucketExist: {}]", bucketName, doesBucketExist);

                if (!doesBucketExist) {
                    log.info("[saveTerminalRecordContent] [create bucket] [name: {}]", bucketName);
                    s3Client.createBucket(bucketName);
                }

                byte[] content = record.getContent();
                ByteArrayInputStream input = null;
                try {
                    ObjectMetadata metadata = new ObjectMetadata();
                    metadata.setContentType("application/json");
                    metadata.setContentLength(content.length);
                    metadata.setHeader("x-hos-meta-message", "message");

                    input = new ByteArrayInputStream(content);

                    // 以流方式上传对象，指定桶名、对象名、输入流、ObjectMetadata
                    s3Client.putObject(bucketName, key, input, metadata);

                    log.info("[saveTerminalRecordContent] [remote storage record succeeded] [key: {}]", key);
                } catch (Exception e) {
                    log.error(e, "[saveTerminalRecordContent] [upload record content to s3 error] [uuid: {}] [start: {}] [size: {}]", uuid, startTime, content.length);
                } finally {
                    Tool.IoUtil.close(input);
                    // 关闭 AmazonS3Client
                    s3Client.shutdown();
                }
                break;
            }
            default:
                break;
        }
    }

    /**
     * build Terminal Storage S3 Client
     *
     * @return
     */
    @Override
    public AmazonS3 buildTerminalStorageS3Client() {
        log.info("[buildTerminalStorageS3Client] [build Amazon S3 Client begin...]");

        String s3Endpoint = sysConfService.getValue(TerminalConstant.TERMINAL_STORAGE_S3_ENDPOINT);
        String accessKey = sysConfService.getValue(TerminalConstant.TERMINAL_STORAGE_S3_ACCESS_KEY);
        String secretKey = sysConfService.getValue(TerminalConstant.TERMINAL_STORAGE_S3_SECRET_ACCESS_KEY);

        AWSCredentials credentials = new BasicAWSCredentials(accessKey, secretKey);
        ClientConfiguration clientConfig = new ClientConfiguration();
        clientConfig.setProtocol(Protocol.HTTP);

        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withClientConfiguration(clientConfig)
                .withEndpointConfiguration(
                        new AwsClientBuilder.EndpointConfiguration(s3Endpoint, null)
                )
                .build();

        log.info("[buildTerminalStorageS3Client] [build Amazon S3 Client finshed] [ClientInfo: {}]", JSONPath.read(JSONUtil.toJsonStr(s3Client), "endpoint"));
        return s3Client;
    }
}
