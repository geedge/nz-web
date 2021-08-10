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
    
    public static String getMfaKey(String key){
    	return "mfa:" + key;
    }

}
