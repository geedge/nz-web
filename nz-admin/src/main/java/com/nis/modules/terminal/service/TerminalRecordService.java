package com.nis.modules.terminal.service;

import com.amazonaws.services.s3.AmazonS3;
import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.terminal.entity.TerminalRecord;

import java.util.Map;

public interface TerminalRecordService extends IService<TerminalRecord> {

    Map queryTerminalRecordInfos(Map<String, Object> params);

    /**
     * save Terminal Record Content
     *
     * @param record
     */
    void saveTerminalRecordContent(TerminalRecord record);

    /**
     * build Terminal Storage S3 Client
     *
     * @return
     */
    AmazonS3 buildTerminalStorageS3Client();
}
