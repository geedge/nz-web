package com.nis.modules.sys.service.impl;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.nis.modules.sys.dao.PermissionInfoDao;
import com.nis.modules.sys.entity.PermissionInfoEntity;
import com.nis.modules.sys.service.PermissionInfoService;

@Service("permissionInfoService")
public class PermissionInfoServiceImpl extends ServiceImpl<PermissionInfoDao, PermissionInfoEntity> implements PermissionInfoService {

}
