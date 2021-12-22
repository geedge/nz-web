package com.nis.common.update.v3_0_to_3_2;

import org.springframework.stereotype.Component;

import com.nis.common.update.VersionUpdateInterface;

/**
 * 记录升级内容
 * 1、类名按照 版本号定义，如：V3_2_1 ，实际版本为 3.2.1
 * 2、flyway执行后将 按照 类名顺序执行
 *  
 */
@Component
public class V3_2Update implements VersionUpdateInterface {

	@Override
	public boolean preUpdate() {
		
		return VersionUpdateInterface.super.preUpdate();
	}
	
	@Override
	public void excute() {

	}

}
