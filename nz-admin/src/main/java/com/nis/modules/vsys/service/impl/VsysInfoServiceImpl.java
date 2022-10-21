package com.nis.modules.vsys.service.impl;

import cn.hutool.log.Log;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.vsys.dao.VsysInfoDao;
import com.nis.modules.vsys.entity.VsysInfo;
import com.nis.modules.vsys.service.VsysInfoService;
import org.springframework.stereotype.Service;

@Service
public class VsysInfoServiceImpl extends ServiceImpl<VsysInfoDao, VsysInfo> implements VsysInfoService {

    private static final Log log = Log.get();


}
