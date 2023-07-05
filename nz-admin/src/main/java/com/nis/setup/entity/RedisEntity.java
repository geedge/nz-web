package com.nis.setup.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class RedisEntity implements Serializable {
    private String mode = "standalone";
    private String[] url;
    private String masterName = "";
    private String database = "0";
    private String pin = "";
}
