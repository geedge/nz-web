/**

 *

 *

 */

package com.nis.common.utils;

/**
 * 常量
 *

 */
public class Constant {
	/** 超级管理员ID */
	public static final int SUPER_ADMIN = 1;
    /** 数据权限过滤 */
	public static final String SQL_FILTER = "sql_filter";
    /**
     * 当前页码
     */
    public static final String PAGE = "page";
    /**
     * 每页显示记录数
     */
    public static final String LIMIT = "limit";
    /**
     * 排序字段
     */
    public static final String ORDER_FIELD = "sidx";
    /**
     * 排序方式
     */
    public static final String ORDER = "order";
    /**
     *  升序
     */
    public static final String ASC = "asc";

	/**
	 * 菜单类型
	 */
    public enum MenuType {
        /**
         * 目录
         */
    	CATALOG(0),
        /**
         * 菜单
         */
        MENU(1),
        /**
         * 按钮
         */
        BUTTON(2);

        private int value;

        MenuType(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    
    /**
     * 定时任务状态
     */
    public enum ScheduleStatus {
        /**
         * 正常
         */
    	NORMAL(0),
        /**
         * 暂停
         */
    	PAUSE(1);

        private int value;

        ScheduleStatus(int value) {
            this.value = value;
        }
        
        public int getValue() {
            return value;
        }
    }

    /**
     * 云服务商
     */
    public enum CloudService {
        /**
         * 七牛云
         */
        QINIU(1),
        /**
         * 阿里云
         */
        ALIYUN(2),
        /**
         * 腾讯云
         */
        QCLOUD(3);

        private int value;

        CloudService(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public static final Long GLOBAL_ID = 1l;
    
    //delFlag
    public static final String DELFLAG_TRUE = "1"; //删除
    public static final String DELFLAG_FALSE = "0"; //未删除
    
    //permission type
    public static final String PM_TYPE_DETESET = "2";
    public static final String PM_TYPE_DETEWARN = "3";
    public static final String PM_TYPE_MISSION = "4";
    
    //view level
    public static final String VIEWLEVEL_UG = "1"; //usergroup
    public static final String VIEWLEVEL_SYS = "2";
    public static final String VIEWLEVEL_ALL = "3";
    
    public static final Long DETESET_NODE_RELTYPE_NODE = 1L;
    public static final Long DETESET_NODE_RELTYPE_NODEGROUP = 2L;

    //mission result state
    public static final String MISSION_RESULT_SUCCESS = "1";  //成功
    public static final String MISSION_RESULT_FAIL = "2";  //失败
    public static final String MISSION_RESULT_TIMEOUT = "3";  //超时

    // mission loop
    public static final String MISSION_LOOP_TRUE = "1";   //周期任务
    public static final String MISSION_LOOP_false = "0";   //周期任务

    public static final String STR_PARAM_SEPRATOR=";";

    // mission node rel
    public static final String MISSION_NODE_REL_TYPE_GROUP="1";
    public static final String MISSION_NODE_REL_TYPE_NODE="2";

    //topo图默认宽高
    public static final Integer TOPO_DEFAULT_HEIGHT = 1600;
    public static final Integer TOPO_DEFAULT_WIDTH = 1600;

    public static final String WARN_REL_TYPE="1";
    public static final String WARN_REL_SET="2";
}
