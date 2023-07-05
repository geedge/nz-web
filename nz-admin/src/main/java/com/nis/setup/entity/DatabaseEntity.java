package com.nis.setup.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class DatabaseEntity implements Serializable {
    private String[] url;
    private String name = "nz";
    private String username;
    private String pin;

    public String getConnectionUrl(String hostAndPort) {
        return "jdbc:mysql://" + hostAndPort + "/" + "?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useSSL=false";
    }

}
