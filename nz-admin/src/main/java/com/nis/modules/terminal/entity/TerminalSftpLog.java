package com.nis.modules.terminal.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * terminal_sftp_log
 *
 * @author shizhendong
 * @date 2022-10-31
 */
@Data
@TableName("terminal_sftp_log")
@NoArgsConstructor
public class TerminalSftpLog {

    @TableId
    private Integer id;

    /**
     * session id
     */
    private String uuid;

    /**
     * type
     * 1: 上传文件
     * 2：下载文件
     * 3：新建文件夹
     * 4：删除文件
     */
    private Integer type;

    /**
     * 文件名
     */
    private String name;

    /**
     * 时间戳
     */
    private Long ts;

    public TerminalSftpLog(String uuid, Integer type, String name, Long ts) {
        this.uuid = uuid;
        this.type = type;
        this.name = name;
        this.ts = ts;
    }
}
