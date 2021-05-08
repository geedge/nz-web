package com.nis.setup.entity;

import java.io.Serializable;

import lombok.Data;
@Data
public class SetupEntity implements Serializable {
    private RedisEntity redis;
    private DatabaseEntity database;
    private SystemEntity system;
    private String code;
}
