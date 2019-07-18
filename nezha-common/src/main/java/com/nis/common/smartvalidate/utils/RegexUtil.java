package com.nis.common.smartvalidate.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ZhangGang on 2017/8/31.
 */
public class RegexUtil {
    public static boolean test(String regex, String value) {
        Pattern p = Pattern.compile(regex);
        Matcher m = p.matcher(value);
        return m.matches();
    }
}
