package com.nis.setup.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class RedisEntity implements Serializable {
    private String host;
    private Integer port = 6379;
    private String pin ;
}
