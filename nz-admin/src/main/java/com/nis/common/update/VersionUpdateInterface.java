package com.nis.common.update;

import cn.hutool.log.Log;

public interface VersionUpdateInterface {
	static final Log log = Log.get();
	/**
	 * 版本升级前操作内容
	 * @return
	 */
	default boolean preUpdate() {
		return true;
	};

	/**
	 * 执行版本升级
	 */
	void excute() throws Exception;

	/**
	 * 版本升级
	 * 1、升级前准备工作
	 * 2、开始升级
	 * 3、释放资源
	 */
	default void update() {
		try {
			boolean pre = preUpdate();
			log.debug("prehandle result: {}", pre);
			if (pre) {
				excute();
			}
		} catch (Exception e) {
			onException(e);
		} finally {
			afterUpdate();
		}
	};

	default void onException(Exception e) {
		log.error(e,"version update error");
	}
	
	
	/**
	 * 升级后操作，可用于释放资源
	 */
	default void afterUpdate() {
		
	};

}
