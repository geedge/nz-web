package com.nis.modules.node.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.node.dao.SystemNodeRelDao;
import com.nis.modules.node.entity.SystemNodeRelEntity;
import com.nis.modules.node.service.SystemNodeRelService;
import org.springframework.stereotype.Service;


@Service("systemNodeRelService")
public class SystemNodeRelServiceImpl extends ServiceImpl<SystemNodeRelDao, SystemNodeRelEntity> implements SystemNodeRelService {

}
