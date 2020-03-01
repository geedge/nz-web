package com.nis.modules.terminal.config;

import java.io.File;

public class Constants{
    public static final String PARAM_HTTP_SESSION_ID_KEY = "http_session_id";
    public static final String CHARSET_UTF8 = "UTF-8";
    public static final String NZ_USER_HOME = System.getProperty("user.home") + File.separator + ".nezha";
    public static final String PARAM_TERMINAL_PREFIX_KEY = "nezha_term_";
    public enum SshType {
        ACCOUNT(0x1),
        SSHKEY(0x2);

        private Integer type;

        SshType(Integer type) {
            this.type = type;
        }

        public static SshType getByType(Integer type) {
            for (SshType sshType : SshType.values()) {
                if (sshType.getType().equals(type)) {
                    return sshType;
                }
            }
            return null;
        }

        public Integer getType() {
            return type;
        }

        public void setType(Integer type) {
            this.type = type;
        }
    }
}