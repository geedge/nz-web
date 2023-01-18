package com.nis.modules.terminal.entity;

import lombok.Data;

@Data
public class TerminalConfigDto {

    /**
     * session id
     */
    private String uuid;

    /**
     * 窗口宽度
     */
    private Integer height;

    /**
     * 窗口高度
     */
    private Integer width;

    /**
     * 显示的列数
     */
    private Integer cols;

    /**
     * 显示的行数
     */
    private Integer rows;

    /**
     * asset id, 与其他连接信息参数互斥，
     * 1. 不能同时为空
     * 2. 同时存在时，以 asset id 为主
     */
    private Integer assetId;

    private String host;

    private Integer port;

    private Integer authProtocol;

    private Integer authProtocolPort;

    private Integer authType;

    private String authUsername;

    private String authPin;

    private String authPriKey;

    private String authUserTip;

    private String authPinTip;

}
