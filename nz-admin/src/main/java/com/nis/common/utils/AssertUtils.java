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


import java.util.Collection;


/**
 * @author <a href="mailto:benjobs@qq.com">Wanghuajie</a>
 * @name:AssertUtils
 * @version: 1.0.0
 * @company: damai
 * @description: 断言工具类
 * @date: 2013-09-9 pa 11:03<br/><br/>
 */

@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class AssertUtils {

    /**
     * Assert a boolean expression, throwing <code>IllegalArgumentException</code>
     * if the test result is <code>false</code>.
     * <pre class="code">AssertUtils.isTrue(i &gt; 0, "The value must be greater than zero");</pre>
     *
     * @param expression a boolean expression
     * @param message    the exception message to use if the assertion fails
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void isTrue(boolean expression, String message) {
        if (!expression) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert a boolean expression, throwing <code>IllegalArgumentException</code>
     * if the test result is <code>false</code>.
     * <pre class="code">AssertUtils.isTrue(i &gt; 0);</pre>
     *
     * @param expression a boolean expression
     * @throws IllegalArgumentException if expression is <code>false</code>
     */
    public static void isTrue(boolean expression) {
        isTrue(expression, "[Assertion failed] - this expression must be true");
    }




    /**
     * Assert that an object is <code>null</code> .
     * <pre class="code">AssertUtils.isNull(value, "The value must be null");</pre>
     *
     * @param object  the object to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object is not <code>null</code>
     */
    public static void isNull(Object object, String message) {
        if (object != null) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that an object is <code>null</code> .
     * <pre class="code">AssertUtils.isNull(value);</pre>
     *
     * @param object the object to check
     * @throws IllegalArgumentException if the object is not <code>null</code>
     */
    public static void isNull(Object object) {
        isNull(object, "[Assertion failed] - the object argument must be null");
    }

    /**
     * Assert that an object is not <code>null</code> .
     * <pre class="code">AssertUtils.notNull(clazz, "The class must not be null");</pre>
     *
     * @param object  the object to check
     * @param message the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the object is <code>null</code>
     */
    public static void notNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void notNull(Object... object) {
        notNull(object, "[Assertion failed] - this argument is required; it must not be null");
        for (int i = 0; i < object.length; i++) {
            notNull(object[i], "[Assertion failed] - this argument index of " + i + " is required; it must not be null");
        }
    }

    /**
     * Assert that an object is not <code>null</code> .
     * <pre class="code">AssertUtils.notNull(clazz);</pre>
     *
     * @param object the object to check
     * @throws IllegalArgumentException if the object is <code>null</code>
     */
    public static void notNull(Object object) {
        notNull(object, "[Assertion failed] - this argument is required; it must not be null");
    }


    /**
     * Assert that the given String has valid text content; that is, it must not
     * be <code>null</code> and must contain at least one non-whitespace character.
     * <pre class="code">AssertUtils.hasText(name, "'name' must not be empty");</pre>
     *
     * @param text    the String to check
     * @param message the exception message to use if the assertion fails
     */
    public static void hasText(String text, String message) {
        if (CommonUtils.isEmpty(text)) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Assert that the given String has valid text content; that is, it must not
     * be <code>null</code> and must contain at least one non-whitespace character.
     * <pre class="code">AssertUtils.hasText(name, "'name' must not be empty");</pre>
     *
     * @param text the String to check
     */
    public static void hasText(String text) {
        hasText(text,
                "[Assertion failed] - this String argument must have text; it must not be null, empty, or blank");
    }


    /**
     * Assert that a collection has elements; that is, it must not be
     * <code>null</code> and must have at least one element.
     * <pre class="code">AssertUtils.notEmpty(collection, "Collection must have elements");</pre>
     *
     * @param collection the collection to check
     * @param message    the exception message to use if the assertion fails
     * @throws IllegalArgumentException if the collection is <code>null</code> or has no elements
     */
    public static void notEmpty(Collection collection, String message) {
        if (CommonUtils.isEmpty(collection)) {
            throw new IllegalArgumentException(message);
        }
    }








    /**
     * Assert a boolean expression, throwing <code>IllegalStateException</code>
     * if the test result is <code>false</code>. Call isTrue if you wish to
     * throw IllegalArgumentException on an assertion failure.
     * <pre class="code">AssertUtils.state(id == null, "The id property must not already be initialized");</pre>
     *
     * @param expression a boolean expression
     * @param message    the exception message to use if the assertion fails
     * @throws IllegalStateException if expression is <code>false</code>
     */
    public static void state(boolean expression, String message) {
        if (!expression) {
            throw new IllegalStateException(message);
        }
    }

    /**
     * Assert a boolean expression, throwing {@link IllegalStateException}
     * if the test result is <code>false</code>.
     * <p>Call {@link #isTrue(boolean)} if you wish to
     * throw {@link IllegalArgumentException} on an assertion failure.
     * <pre class="code">AssertUtils.state(id == null);</pre>
     *
     * @param expression a boolean expression
     * @throws IllegalStateException if the supplied expression is <code>false</code>
     */
    public static void state(boolean expression) {
        state(expression, "[Assertion failed] - this state invariant must be true");
    }





}
