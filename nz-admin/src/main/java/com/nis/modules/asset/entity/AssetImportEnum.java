package com.nis.modules.asset.entity;


/**
 * asset 批量导入，导入属性&下标枚举类
 */
public enum AssetImportEnum {

    NAME("name", 0),
    SN("sn", 1),
    PARENT("parent", 2),
    MANAGE_IP("manageIp", 3),
    BRAND("brand", 4),
    MODEL("model", 5),
    DC("dc", 6),
    CABINET("cabinet", 7),
    U_START("ustart", 8),
    U_END("uend", 9),
    STATE("stateConf", 10),
    TYPE("typeConf", 11),
    PURCHASE_DATE("purchaseDate", 12),
    TAG("tag", 13),
    AUTH_PROTOCOL_PORT("authProtocolPort", 14),
    AUTH_TYPE("authType", 15),
    AUTH_USER_NAME("authUsername", 16),
    AUTH_PIN("authPin", 17),
    AUTH_PRI_KEY("authPriKey", 18),
    AUTH_USER_TIP("authUserTip", 19),
    AUTH_PIN_TIP("authPinTip", 20),
    SNMP_CREDENTIAL("snmpCredential", 21),
    CLIENT_USER("clientUser", 22),
    CLIENT_PIN("clientPin", 23),
    CLIENT_TOKEN("clientToken", 24),
    CLIENT_PORT("clientPort", 25),
    CLIENT_ENABLE("clientEnable", 26);

    private String name;
    private int index;

    AssetImportEnum(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

}
