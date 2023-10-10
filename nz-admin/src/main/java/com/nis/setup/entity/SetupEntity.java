package com.nis.setup.entity;

import lombok.Data;

import java.io.Serializable;
@Data
public class SetupEntity implements Serializable {
    private RedisEntity redis;
    private DatabaseEntity database;
    private SystemEntity system;
    private String code;
}
