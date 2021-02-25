/**
 * Copyright (c) 2015 The Nezha Project
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.nis.common.utils;

/**
 * @Package: cn.damai.usercenter.common.util
 * @Description:
 * @author: <a href="mailto:benjobs@qq.com">Wanghuajie</a>
 * @date: 13-3-6 - 上午11:17
 * @version: V1.0
 * @company: damai
 */

import java.util.Arrays;

public abstract class ObjectUtils {

    private static final int INITIAL_HASH = 7;
    private static final int MULTIPLIER = 31;

    private static final String EMPTY_STRING = "";
    private static final String NULL_STRING = "null";
    private static final String ARRAY_START = "{";
    private static final String ARRAY_END = "}";
    private static final String EMPTY_ARRAY = ARRAY_START + ARRAY_END;
    private static final String ARRAY_ELEMENT_SEPARATOR = ", ";


    /**
     * Return whether the given throwable is a checked exception:
     * that is, neither a RuntimeException nor an Error.
     *
     * @param ex the throwable to check
     * @return whether the throwable is a checked exception
     * @see Exception
     * @see RuntimeException
     * @see Error
     */
    public static boolean isCheckedException(Throwable ex) {
        return !(ex instanceof RuntimeException || ex instanceof Error);
    }


    /**
     * Determine whether the given object is an array:
     * either an Object array or a primitive array.
     *
     * @param obj the object to check
     */
    public static boolean isArray(Object obj) {
        return (obj != null && obj.getClass().isArray());
    }

    /**
     * Determine whether the given array is empty:
     * i.e. <code>null</code> or of zero length.
     *
     * @param array the array to check
     */
    public static boolean isEmpty(Object[] array) {
        return (array == null || array.length == 0);
    }





    //---------------------------------------------------------------------
    // Convenience methods for content-based equality/hash-code handling
    //---------------------------------------------------------------------

    /**
     * Determine if the given objects are equal, returning <code>true</code>
     * if both are <code>null</code> or <code>false</code> if only one is
     * <code>null</code>.
     * <p>Compares arrays with <code>Arrays.equals</code>, performing an equality
     * check based on the array elements rather than the array reference.
     *
     * @param o1 first Object to compare
     * @param o2 second Object to compare
     * @return whether the given objects are equal
     * @see Arrays#equals
     */
    public static boolean safeEquals(Object o1, Object o2) {
        if (o1 == null || o2 == null) {
            return false;
        }

        if (o1 == o2) {
            return true;
        }

        if (o1.equals(o2)) {
            return true;
        }
        if (o1.getClass().isArray() && o2.getClass().isArray()) {
            if (o1 instanceof Object[] && o2 instanceof Object[]) {
                return Arrays.equals((Object[]) o1, (Object[]) o2);
            }
            if (o1 instanceof boolean[] && o2 instanceof boolean[]) {
                return Arrays.equals((boolean[]) o1, (boolean[]) o2);
            }
            if (o1 instanceof byte[] && o2 instanceof byte[]) {
                return Arrays.equals((byte[]) o1, (byte[]) o2);
            }
            if (o1 instanceof char[] && o2 instanceof char[]) {
                return Arrays.equals((char[]) o1, (char[]) o2);
            }
            if (o1 instanceof double[] && o2 instanceof double[]) {
                return Arrays.equals((double[]) o1, (double[]) o2);
            }
            if (o1 instanceof float[] && o2 instanceof float[]) {
                return Arrays.equals((float[]) o1, (float[]) o2);
            }
            if (o1 instanceof int[] && o2 instanceof int[]) {
                return Arrays.equals((int[]) o1, (int[]) o2);
            }
            if (o1 instanceof long[] && o2 instanceof long[]) {
                return Arrays.equals((long[]) o1, (long[]) o2);
            }
            if (o1 instanceof short[] && o2 instanceof short[]) {
                return Arrays.equals((short[]) o1, (short[]) o2);
            }
        }
        return false;
    }

    /**
     * Return as hash code for the given object; typically the value of
     * <code>{@link Object#hashCode()}</code>. If the object is an array,
     * this method will delegate to any of the <code>safeHashCode</code>
     * methods for arrays in this class. If the object is <code>null</code>,
     * this method returns 0.
     *
     * @see #safeHashCode(Object[])
     * @see #safeHashCode(boolean[])
     * @see #safeHashCode(byte[])
     * @see #safeHashCode(char[])
     * @see #safeHashCode(double[])
     * @see #safeHashCode(float[])
     * @see #safeHashCode(int[])
     * @see #safeHashCode(long[])
     * @see #safeHashCode(short[])
     */
    public static int safeHashCode(Object obj) {
        if (obj == null) {
            return 0;
        }
        if (obj.getClass().isArray()) {
            if (obj instanceof Object[]) {
                return safeHashCode((Object[]) obj);
            }
            if (obj instanceof boolean[]) {
                return safeHashCode((boolean[]) obj);
            }
            if (obj instanceof byte[]) {
                return safeHashCode((byte[]) obj);
            }
            if (obj instanceof char[]) {
                return safeHashCode((char[]) obj);
            }
            if (obj instanceof double[]) {
                return safeHashCode((double[]) obj);
            }
            if (obj instanceof float[]) {
                return safeHashCode((float[]) obj);
            }
            if (obj instanceof int[]) {
                return safeHashCode((int[]) obj);
            }
            if (obj instanceof long[]) {
                return safeHashCode((long[]) obj);
            }
            if (obj instanceof short[]) {
                return safeHashCode((short[]) obj);
            }
        }
        return obj.hashCode();
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If <code>array</code> is <code>null</code>, this method returns 0.
     */
    public static int safeHashCode(Object[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (Object anArray : array) {
            hash = MULTIPLIER * hash + safeHashCode(anArray);
        }
        return hash;
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If <code>array</code> is <code>null</code>, this method returns 0.
     */
    public static int safeHashCode(boolean[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (boolean anArray : array) {
            hash = MULTIPLIER * hash + hashCode(anArray);
        }
        return hash;
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If <code>array</code> is <code>null</code>, this method returns 0.
     */
    public static int safeHashCode(byte[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (byte anArray : array) {
            hash = MULTIPLIER * hash + anArray;
        }
        return hash;
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If <code>array</code> is <code>null</code>, this method returns 0.
     */
    public static int safeHashCode(char[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (char anArray : array) {
            hash = MULTIPLIER * hash + anArray;
        }
        return hash;
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If <code>array</code> is <code>null</code>, this method returns 0.
     */
    public static int safeHashCode(double[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (double anArray : array) {
            hash = MULTIPLIER * hash + hashCode(anArray);
        }
        return hash;
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If <code>array</code> is <code>null</code>, this method returns 0.
     */
    public static int safeHashCode(float[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (float anArray : array) {
            hash = MULTIPLIER * hash + hashCode(anArray);
        }
        return hash;
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If <code>array</code> is <code>null</code>, this method returns 0.
     */
    public static int safeHashCode(int[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (int anArray : array) {
            hash = MULTIPLIER * hash + anArray;
        }
        return hash;
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If <code>array</code> is <code>null</code>, this method returns 0.
     */
    public static int safeHashCode(long[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (long anArray : array) {
            hash = MULTIPLIER * hash + hashCode(anArray);
        }
        return hash;
    }

    /**
     * Return a hash code based on the contents of the specified array.
     * If <code>array</code> is <code>null</code>, this method returns 0.
     */
    public static int safeHashCode(short[] array) {
        if (array == null) {
            return 0;
        }
        int hash = INITIAL_HASH;
        for (short anArray : array) {
            hash = MULTIPLIER * hash + anArray;
        }
        return hash;
    }

    /**
     * Return the same value as <code>{@link Boolean#hashCode()}</code>.
     *
     * @see Boolean#hashCode()
     */
    public static int hashCode(boolean bool) {
        return bool ? 1231 : 1237;
    }

    /**
     * Return the same value as <code>{@link Double#hashCode()}</code>.
     *
     * @see Double#hashCode()
     */
    public static int hashCode(double dbl) {
        long bits = Double.doubleToLongBits(dbl);
        return hashCode(bits);
    }

    /**
     * Return the same value as <code>{@link Float#hashCode()}</code>.
     *
     * @see Float#hashCode()
     */
    public static int hashCode(float flt) {
        return Float.floatToIntBits(flt);
    }

    /**
     * Return the same value as <code>{@link Long#hashCode()}</code>.
     *
     * @see Long#hashCode()
     */
    public static int hashCode(long lng) {
        return (int) (lng ^ (lng >>> 32));
    }




}

