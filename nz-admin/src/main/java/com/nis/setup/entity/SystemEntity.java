package com.nis.setup.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class SystemEntity implements Serializable {
    private String alertPath;
    private String alertPrefix;
    // prometheus_federation_enabled
    private String federationEnabled = "1";
    private String username;
    private String pin;
}
