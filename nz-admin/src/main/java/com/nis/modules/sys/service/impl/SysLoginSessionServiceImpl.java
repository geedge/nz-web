package com.nis.modules.sys.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.sys.dao.SysLoginSessionDao;
import com.nis.modules.sys.entity.SysLoginSession;
import com.nis.modules.sys.service.SysLoginSessionService;
import org.springframework.stereotype.Service;

@Service
public class SysLoginSessionServiceImpl extends ServiceImpl<SysLoginSessionDao, SysLoginSession> implements SysLoginSessionService {
}
