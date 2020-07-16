/**
 *
 */

package com.nis.common.xss;

import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;
import org.apache.commons.lang.StringUtils;

import java.util.regex.Pattern;

/**
 * SQL过滤
 *
 * @author Mark sunlightcs@gmail.com
 */
public class SQLFilter {

    private static String reg = "(?:')|(?:--)|(/\\*(?:.|[\\n\\r])*?\\*/)|(\\b(select|update|union|and|or|delete|insert|trancate|char|into|substr|ascii|declare|exec|count|master|into|drop|execute)\\b)";

    private static Pattern sqlPattern = Pattern.compile(reg, Pattern.CASE_INSENSITIVE);

    /**
     * SQL注入过滤
     * @param str  待验证的字符串
     */
    public static String sqlInject(String str) {
        if (StringUtils.isBlank(str)) {
            return null;
        }

        //转换成小写
        str = str.toLowerCase();

        String s = "";
        if (str.startsWith("-")) {
            s = str.substring(1);
        } else {
            s = str;
        }

        if (sqlPattern.matcher(s).matches()) {
            throw new NZException(RCode.ERROR);
        }

        return str;
    }
}
