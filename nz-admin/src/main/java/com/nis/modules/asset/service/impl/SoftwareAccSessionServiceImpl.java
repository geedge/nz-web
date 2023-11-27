package com.nis.modules.asset.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.asset.dao.SoftwareAccSessionDao;
import com.nis.modules.asset.entity.SoftwareAccSession;
import com.nis.modules.asset.service.SoftwareAccSessionService;
import org.springframework.stereotype.Service;

/**
 * software acc session service implement
 *
 * @author shizhendong
 * @date 2023-11-24
 */
@Service
public class SoftwareAccSessionServiceImpl extends ServiceImpl<SoftwareAccSessionDao, SoftwareAccSession> implements SoftwareAccSessionService {

    private static final Log log = Log.get();

}
