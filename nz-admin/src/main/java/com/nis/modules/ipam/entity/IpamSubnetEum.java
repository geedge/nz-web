package com.nis.modules.ipam.entity;

public enum IpamSubnetEum {
    NAME("name", 0),
    TYPE("type", 1),
    ADDR("addr", 2),
    MASK("mask", 3),
    VLAN("vlan", 4),
    DC("dc id", 5),
    REMARK("remark", 6);

    private String name;
    private int index;

    IpamSubnetEum(String name, int index) {
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
