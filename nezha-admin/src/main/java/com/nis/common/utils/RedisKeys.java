/**

 *

 *

 */

package com.nis.common.utils;

/**
 * Redis所有Keys
 *

 */
public class RedisKeys {

    public static String getSysConfigKey(String key){
        return "sys:config:" + key;
    }

    public static String getShiroSessionKey(String key){
        return "sessionid:" + key;
    }
    
    public static String getDeteTypeKey(String key) {
    	return "dete:type:" + key;
    }
    
    public static String getDeteSetKey(String key) {
    	return "dete:set:" + key;
    }
    
    public static String getDeteWarnKey(String key) {
    	return "dete:set:" + key;
    }
    
    public static String getNodeGroupKey(String key) {
    	return "node:group:" + key;
    }
    
    public static String getNodeKey(String key) {
    	return "node:" + key;
    }
}
