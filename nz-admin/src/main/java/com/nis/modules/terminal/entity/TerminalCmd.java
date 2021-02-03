package com.nis.modules.terminal.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * terminal_cmd
 *
 * @author shizhendong
 * @date 2021-02-01
 */
@Data
@TableName("terminal_cmd")
public class TerminalCmd {

    @TableId
    private Integer id;

    /**
     * session id
     */
    private String uuid;

    /**
     * 命令
     */
    private String cmd;

    /**
     * 单位：ms
     *
     * 记录的是 session 开始后的时间偏移量
     */
    private Integer time;

    @Override
    public String toString() {
        return "TerminalCmd{" +
                "id=" + id +
                ", uuid='" + uuid + '\'' +
                ", cmd='" + cmd + '\'' +
                ", time=" + time +
                '}';
    }
}