package com.nis.setup.entity;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
public class SystemEntity implements Serializable {
    private String alertPath;
    private String alertPrefix;
    private Integer haMode = 1;
    private String haVip;
    private String username;
    private String pin;
}
