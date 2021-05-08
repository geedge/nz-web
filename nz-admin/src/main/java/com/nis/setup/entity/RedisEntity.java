package com.nis.setup.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class RedisEntity implements Serializable {
    private String host;
    private Integer port = 6379;
    private String pin ;
}
