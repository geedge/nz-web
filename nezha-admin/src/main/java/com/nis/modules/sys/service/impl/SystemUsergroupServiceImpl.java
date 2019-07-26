package com.nis.modules.sys.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.sys.dao.SystemUsergroupDao;
import com.nis.modules.sys.entity.SystemUsergroupEntity;
import com.nis.modules.sys.service.SystemUsergroupService;

@Service("systemUsergroupService")
public class SystemUsergroupServiceImpl extends ServiceImpl<SystemUsergroupDao, SystemUsergroupEntity> implements SystemUsergroupService {

}
