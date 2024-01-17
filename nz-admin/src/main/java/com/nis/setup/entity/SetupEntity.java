package com.nis.setup.entity;

import java.io.Serializable;

import com.nis.modules.sys.entity.SysUserEntity;
import lombok.Data;
@Data
public class SetupEntity implements Serializable {
    private DatabaseEntity database;
    private SystemEntity system;
    private SysUserEntity adminUser;
    private String code;
}
