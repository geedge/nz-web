package com.nis.common.utils.SqlHelper;

public enum KeywordsType {
    STRING("string"),//字符串类型
    NUMBER("number"),//普通数字类型
    DATE("date"),//时间戳类型
    PRIMARYKEY("primaryKey"),//主键类型，项目内主键统一为Long类型
    COMPUTE("compute");//计算类型


    private String key;

    KeywordsType(String key){
        this.key=key;
    }
}
