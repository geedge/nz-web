package com.nis.modules.promserver.util;

public class PromConstant {

    // query_range series 查询结果条数限制
    public static Integer queryMaxSeries;

    /**
     * global prometheus type
     */
    public static final Integer PROMSERVER_GLOBAL = 1;

    /**
     * per_datacenter type
     */
    public static final Integer PROMSERVER_PER_DATACENTER = 2;

    /**
     * thanos_query type
     */
    public static final Integer PROMSERVER_THANOS_QUERY = 3;

}
