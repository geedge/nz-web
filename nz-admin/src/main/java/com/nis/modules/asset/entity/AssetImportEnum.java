package com.nis.modules.asset.entity;


/**
 * asset 批量导入，导入属性&下标枚举类
 */
public enum AssetImportEnum {

    NAME("name", 0),
    SN("sn", 1),
    PARENT("parent", 2),
    MANAGE_IP("manageIp", 3),
    IPMI_IP("ipmiIp", 4),
    BRAND("brand", 5),
    MODEL("model", 6),
    DC("dc", 7),
    CABINET("cabinet", 8),
    U_START("ustart", 9),
    U_END("uend", 10),
    STATE("stateConf", 11),
    TYPE("typeConf", 12),
    PURCHASE_DATE("purchaseDate", 13),
    TAG("tag", 14),
    AUTH_PROTOCOL_PORT("authProtocolPort", 15),
    AUTH_TYPE("authType", 16),
    AUTH_USER_NAME("authUsername", 17),
    AUTH_PIN("authPin", 18),
    AUTH_PRI_KEY("authPriKey", 19),
    AUTH_USER_TIP("authUserTip", 20),
    AUTH_PIN_TIP("authPinTip", 21),
    SNMP_CREDENTIAL("snmpCredential", 22),
    CLIENT_USER("clientUser", 23),
    CLIENT_PIN("clientPin", 24),
    CLIENT_TOKEN("clientToken", 25),
    CLIENT_PORT("clientPort", 26),
    CLIENT_ENABLE("clientEnable", 27);

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
