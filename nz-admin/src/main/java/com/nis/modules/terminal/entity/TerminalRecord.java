package com.nis.modules.terminal.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * terminal_record
 *
 * @author shizhendong
 * @date 2021-02-01
 */
@Data
@TableName("terminal_record")
public class TerminalRecord {

    @TableId
    private Integer id;

    /**
     * session id
     */
    private String uuid;

    /**
     * 开始时间
     * record 开始时间，记录的是会话开始后的时间偏移量
     */
    private Integer startTime;

    /**
     * 结束时间
     * record 结束时间，记录的是会话开始后的时间偏移量
     */
    private Integer endTime;

    /**
     * 存储模式，1: 本地存储，2：S3对象存储
     */
    private Integer storageType;

    /**
     * S3 path
     */
    private String path;

    /**
     * 文件内容
     * 记录的是 session开始后的时间偏移量
     * 超过定义临时文件大小后，再新增一条记录
     */
    private byte[] content;
}
