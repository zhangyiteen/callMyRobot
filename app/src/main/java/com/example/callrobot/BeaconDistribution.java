package com.example.callrobot;

public class BeaconDistribution {
    private String bid;
    private Integer horizontal;
    private  Integer vertical;

    public BeaconDistribution(String bid, Integer horizontal, Integer vertical) {
        this.bid = bid;
        this.horizontal = horizontal;
        this.vertical = vertical;
    }

    public String getBid() {
        return bid;
    }

    public void setBid(String bid) {
        this.bid = bid;
    }

    public Integer getHorizontal() {
        return horizontal;
    }

    public void setHorizontal(Integer horizontal) {
        this.horizontal = horizontal;
    }

    public Integer getVertical() {
        return vertical;
    }

    public void setVertical(Integer vertical) {
        this.vertical = vertical;
    }





}
