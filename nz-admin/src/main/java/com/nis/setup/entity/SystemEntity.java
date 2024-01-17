package com.nis.setup.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class SystemEntity implements Serializable {
    private String timezone;
    private String language = "en";

    // prometheus_federation_enabled
    private String federationEnabled = "1";
    private String asset_ping_from = "1";
    private String asset_ping_interval = "300";
    private String default_scrape_interval = "30";
    private String default_scrape_timeout = "30";
    private String snmp_trap_listen_port = "162";

}
