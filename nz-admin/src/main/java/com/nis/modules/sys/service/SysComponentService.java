package com.nis.modules.sys.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.nis.modules.sys.entity.SysComponent;

public interface SysComponentService extends IService<SysComponent>{

	void updateComponent(SysComponent component);

	SysComponent findByUrl(String alertApi);
	
}
