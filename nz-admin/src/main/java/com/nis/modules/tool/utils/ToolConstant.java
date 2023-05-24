package com.nis.modules.tool.utils;

import cn.hutool.core.util.StrUtil;

import java.util.regex.Pattern;

public class ToolConstant {

    public static final Pattern NUMBER_OPERATION_PATTERN = Pattern.compile("[-<>=]{1,2}");

    /**
     * full-text support type & table mapping
     */
    public enum KeywordTypeTableEnum {
        RULE("rule", "alert_rule"),

        DC("dc", "dc"),
        ASSET("asset", "asset_asset"),
        BRAND("brand", "asset_brand"),
        MODEL("model", "asset_model"),
        ASSET_TYPE("assetType", "asset_type_conf"),

        PROJECT("project", "monitor_project"),
        MODULE("module", "monitor_module"),
        ENDPOINT("endpoint", "monitor_endpoint");

        private String type;
        private String tableName;

        KeywordTypeTableEnum(String type, String tableName) {
            this.type = type;
            this.tableName = tableName;
        }

        public String getType() {
            return type;
        }

        public String getTableName() {
            return tableName;
        }

        public static KeywordTypeTableEnum getInstanceByType(String type) {
            for (KeywordTypeTableEnum anEnum : KeywordTypeTableEnum.values()) {
                if (StrUtil.equals(anEnum.getType(), type.toLowerCase())) {
                    return anEnum;
                }
            }
            return null;
        }
    }
}
