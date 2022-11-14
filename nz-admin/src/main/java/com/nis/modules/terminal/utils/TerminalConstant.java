package com.nis.modules.terminal.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TerminalConstant {

    public static final String PARAM_HTTP_SESSION_ID_KEY = "http_session_id";
    public static final String NZ_USER_HOME = System.getProperty("user.home") + File.separator + ".nezha";
    public static final String PARAM_TERMINAL_PREFIX_KEY = "nezha_term_";

    /**
     * 单位为 byte , 10485760 byte = 10Mb
     */
    public static final Integer RECORD_TEMPFILE_MAX_LENGTH = 10485760;

    /**
     * 不可见字符
     */
    public static final List<Integer> INVISIBLE_CHAR;

    /**
     * 未登录提示信息
     */
    public static final String UNLOGIN_MESSAGE;

    static {
        INVISIBLE_CHAR = new ArrayList<>();
        INVISIBLE_CHAR.add(7);
        INVISIBLE_CHAR.add(8);
        INVISIBLE_CHAR.add(12);
        INVISIBLE_CHAR.add(10);
        INVISIBLE_CHAR.add(13);
        INVISIBLE_CHAR.add(9);
        INVISIBLE_CHAR.add(11);
        INVISIBLE_CHAR.add(92);
        INVISIBLE_CHAR.add(63);
        INVISIBLE_CHAR.add(39);
        INVISIBLE_CHAR.add(34);
        INVISIBLE_CHAR.add(0);

        StringBuilder sb = new StringBuilder();
        sb.append("\r\nlogout\r\n");
        sb.append("Welcome to Nezha Terminal! \r\n");
        sb.append("Type `help' to learn how to use Nz Terminal prompt. \r\n");
        sb.append("$: ");
        UNLOGIN_MESSAGE = sb.toString();
    }

    /**
     * SSH 认证类型
     */
    public enum SshAuthType {
        // password
        PIN(1, "password"),
        // private key
        KEY(2, "key");

        private Integer type;
        private String value;

        SshAuthType(Integer type, String value) {
            this.type = type;
            this.value = value;
        }


        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public static SshAuthType getEntityByType(Integer type) {
            for (SshAuthType v : values()) {
                if (v.getType().equals(type)) {
                    return v;
                }
            }
            throw new IllegalArgumentException();
        }

        public static SshAuthType getEntityByValue(String value) {
            for (SshAuthType v : values()) {
                if (v.getValue().equals(value)) {
                    return v;
                }
            }
            throw new IllegalArgumentException();
        }
    }

    /**
     * ProtocolType
     */
    public enum ProtocolType {
        // ssh
        SSH(1),
        // telnet
        TELNET(2);

        private Integer value;

        ProtocolType(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public static ProtocolType getEntityByValue(Integer value) {
            for (ProtocolType v : values()) {
                if (v.getValue().equals(value)) {
                    return v;
                }
            }
            throw new IllegalArgumentException();
        }
    }


    public enum SessionStatus {

        /**
         * 连接中
         */
        CONNECTING(0),

        /**
         * 连接失败
         */
        CONNECTION_FAILED(1),

        /**
         * 已结束
         */
        OVER(2),

        /**
         * 强制退出
         */
        FORCE_QUIT(3),

        /**
         * 未知错误
         */
        UNKNOWN_ERROR(4);


        private int value;

        SessionStatus(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    /**
     * SftpOperType
     */
    public enum SftpOperType {
        // upload
        UPLOAD(1),
        // download
        DOWNLOAD(2),
        // cmd mkdir
        MKDIR(3),
        // cmd rm
        RM(4);

        private Integer value;

        SftpOperType(Integer value) {
            this.value = value;
        }

        public Integer getValue() {
            return value;
        }

    }

}
