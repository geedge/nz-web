package com.nis.modules;


import com.nis.modules.sys.SysUserTest;
import org.junit.runner.RunWith;

/**
 * 批量运行测试类，在 @SuiteClasses 中加入需要批量运行的 UT Class 即可
 * 例： @org.junit.runners.Suite.SuiteClasses({
 *         A.class,B.class,C.class
 *       })
 */
@RunWith(org.junit.runners.Suite.class)
@org.junit.runners.Suite.SuiteClasses({
        SysUserTest.class
})
public class RunBatchUT {

}
