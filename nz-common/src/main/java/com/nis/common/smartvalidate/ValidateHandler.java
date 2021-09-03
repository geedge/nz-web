package com.nis.common.smartvalidate;

import com.nis.common.exception.NZException;
import com.nis.common.utils.RCode;

import java.util.Collection;
import java.util.Map;

/**
 * ValidateHandler
 * Created by ZhangGang on 2016/9/27.
 */
public class ValidateHandler {

    public static void notNull(Object value, RCode code) {
        try {
            ValidateProcess.notNull(value);
            if (value instanceof String) {
                ValidateProcess.notNull((String) value);
            } else if (value instanceof Number) {
                ValidateProcess.notNull((Number) value);
            } else if (value instanceof Collection) {
                ValidateProcess.notNull((Collection) value);
            } else if (value instanceof Map) {
                ValidateProcess.notNull((Map) value);
            } else if (value instanceof Object[]) {
                ValidateProcess.notNull((Object[]) value);
            }
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void equalsIgnoreCase(Object value,CharSequence sequence,RCode code) {
    	try {
    		ValidateProcess.equalsIgnoreCase(value, sequence);
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    	
    }
    
    public static void regex(String regex, Object value, RCode code) {
        try {
            if (null == value) {
                return;
            }
            ValidateProcess.regex(regex, value.toString());
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void max(Number max, Object value, RCode code) {
        try {
            if (null == value || !(value instanceof Number)) {
                return;
            }
            if (value instanceof Integer) {
                ValidateProcess.max(max.intValue(), (Integer) value);
            } else if (value instanceof Long) {
                ValidateProcess.max(max.longValue(), (Long) value);
            } else if (value instanceof Double) {
                ValidateProcess.max(max.doubleValue(), (Double) value);
            } else if (value instanceof Float) {
                ValidateProcess.max(max.floatValue(), (Float) value);
            } else if (value instanceof Short) {
                ValidateProcess.max(max.shortValue(), (Short) value);
            } else if (value instanceof Byte) {
                ValidateProcess.max(max.byteValue(), (Byte) value);
            }
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void maxLength(int max, Object value, RCode code) {
        try {
            if (null == value) {
                return;
            }
            if (value instanceof String) {
                ValidateProcess.maxLength(max, (String) value);
            } else if (value instanceof Collection) {
                ValidateProcess.maxLength(max, (Collection) value);
            } else if (value instanceof Map) {
                ValidateProcess.maxLength(max, (Map) value);
            } else if (value instanceof Object[]) {
                ValidateProcess.maxLength(max, (Object[]) value);
            }
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void minLength(int min, Object value, RCode code) {
        try {
            if (null == value) {
                return;
            }
            if (value instanceof String) {
                ValidateProcess.minLength(min, (String) value);
            } else if (value instanceof Collection) {
                ValidateProcess.minLength(min, (Collection) value);
            } else if (value instanceof Map) {
                ValidateProcess.minLength(min, (Map) value);
            } else if (value instanceof Object[]) {
                ValidateProcess.minLength(min, (Object[]) value);
            }
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }


    public static void min(Number min, Object value, RCode code) {
        try {
            if (null == value || !(value instanceof Number)) {
                return;
            }
            if (value instanceof Integer) {
                ValidateProcess.min(min.intValue(), (Integer) value);
            } else if (value instanceof Long) {
                ValidateProcess.min(min.longValue(), (Long) value);
            } else if (value instanceof Double) {
                ValidateProcess.min(min.doubleValue(), (Double) value);
            } else if (value instanceof Float) {
                ValidateProcess.min(min.floatValue(), (Float) value);
            } else if (value instanceof Short) {
                ValidateProcess.min(min.shortValue(), (Short) value);
            } else if (value instanceof Byte) {
                ValidateProcess.min(min.byteValue(), (Byte) value);
            }
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void chinese(Object value, RCode code) {
        try {
            if (null == value) {
                return;
            }
            ValidateProcess.chinese(value.toString());
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void english(Object value, RCode code) {
        try {
            if (null == value) {
                return;
            }
            ValidateProcess.english(value.toString());
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void phone(Object value, RCode code) {
        try {
            if (null == value) {
                return;
            }
            ValidateProcess.phone(value.toString());
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void email(Object value, RCode code) {
        try {
            if (null == value) {
                return;
            }
            ValidateProcess.email(value.toString());
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void date(String format, Object value, RCode code) {
        try {
            if (null != value) {
                ValidateProcess.date(format, value.toString());
            }
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void idCard(Object value, RCode code) {
        try {
            if (null == value) {
                return;
            }
            ValidateProcess.idCard(value.toString());
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    public static void ip(Object value, RCode code) {
        try {
            if (null == value) {
                return;
            }
            ValidateProcess.isIp(value.toString());
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }

    private static void errorMsgHandler(NZException e, RCode code) {
        if (code == null) {
            throw e;
        }
        throw new NZException(code);
    }

    public static void port(Object value, RCode code) {
        try {
            if (null == value) {
                return;
            }
            ValidateProcess.isPort(value.toString());
        } catch (NZException e) {
            errorMsgHandler(e, code);
        }
    }
}
