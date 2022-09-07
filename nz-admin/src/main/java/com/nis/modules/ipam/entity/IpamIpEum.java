package com.nis.modules.ipam.entity;

public enum IpamIpEum {
    NAME("name", 0),
    SUBNET("subnet name", 1),
    ADDR("addr", 2),
    MAC("mac", 3),
    ASSET("asset name", 4),
    REMARK("remark", 5);

    private String name;
    private int index;

    IpamIpEum(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }
}
