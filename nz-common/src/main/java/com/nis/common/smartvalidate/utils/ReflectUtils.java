package com.nis.common.smartvalidate.utils;

import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ZhangGang on 2017/9/26.
 */
public class ReflectUtils {

    public static Set<Field> getFieldsByClass(Class cls) {
        Set<Field> fieldSet = new HashSet<>();
        for (Class<?> clazz = cls; clazz != Object.class; clazz = clazz.getSuperclass()) {
            Field[] fields = clazz.getDeclaredFields();
            if (CommonUtil.isNull(fields)) {
                continue;
            }
            for (Field field : fields) {
                if (!field.getName().equals("class") && !field.getName().equals("serialVersionUID")) {
                    fieldSet.add(field);
                }
            }
        }
        return fieldSet;
    }
}
