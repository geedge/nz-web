package com.nis.modules;

import cn.hutool.log.Log;
import org.junit.Test;

/**
 * @author lxm
 * @date 2022/4/22
 */
public class JunitDemo {

    protected static final Log log = Log.get();

    @Test
    public void test() {
        log.info("设置定时自动单测后，试一下提交代码是否会触发自动测试");
    }
}
