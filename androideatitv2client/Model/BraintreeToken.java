package com.ds.androideatitv2client.Model;

public class BraintreeToken {
    private boolean error;
    private String token;

    public BraintreeToken() {
    }

    public boolean isError() {
        return error;
    }

    public void setError(boolean error) {
        this.error = error;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
