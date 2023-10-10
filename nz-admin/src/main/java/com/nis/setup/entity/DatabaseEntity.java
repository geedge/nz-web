package com.nis.setup.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class DatabaseEntity implements Serializable {
	private String host;
    private Integer port = 3306;
    private String name = "nz";
    private String username;
    private String pin;

    public String getUrl(){
        return "jdbc:mysql://"+this.getHost()+":"+this.getPort()+"/"+"?allowMultiQueries=true&useUnicode=true&characterEncoding=UTF-8&useSSL=false";
    }

}
