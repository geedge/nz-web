package com.nis.modules.sys.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.sys.dao.SysBackupLogDao;
import com.nis.modules.sys.entity.SysBackupLog;
import com.nis.modules.sys.service.SysBackupLogService;
import org.springframework.stereotype.Service;

/**
 * sys backup log service implement
 *
 * @author shizhendong
 * @date 2023-09-19
 */
@Service
public class SysBackupLogServiceImpl extends ServiceImpl<SysBackupLogDao, SysBackupLog> implements SysBackupLogService {

    private static final Log log = Log.get();

}
