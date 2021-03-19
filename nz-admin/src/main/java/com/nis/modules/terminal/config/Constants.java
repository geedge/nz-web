package com.nis.modules.terminal.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Constants {
    public static final String PARAM_HTTP_SESSION_ID_KEY = "http_session_id";
    public static final String CHARSET_UTF8 = "UTF-8";
    public static final String NZ_USER_HOME = System.getProperty("user.home") + File.separator + ".nezha";
    public static final String PARAM_TERMINAL_PREFIX_KEY = "nezha_term_";

    // 不可见字符
    public static final List<Integer> INVISIBLE_CHAR;

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
    }

    public enum SshType {
        ACCOUNT(0x1, "password"),
        SSHKEY(0x2, "key");

        private Integer type;
        private String value;

        SshType(Integer type, String value) {
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

    // 单位为 byte , 10485760 byte = 10Mb
    public static final Integer RECORD_TEMPFILE_MAX_LENGTH = 10485760;
}