package com.ds.androideatitv2client.Model;

public class UserModel {
    private String uid,name, address,phone;
    private double lat,lng;

    public UserModel() {
    }

    public UserModel(String uid, String name, String address, String phone,double lat,double lng) {
        this.uid = uid;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.lat = lat;
        this.lng = lng;

    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }
}
