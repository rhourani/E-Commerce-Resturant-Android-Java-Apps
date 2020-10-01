package com.ds.androideatitv2client.Model;

public class FCMResult {
    private String message_id;

    public FCMResult() {
    }

    public FCMResult(String message_id) {
        this.message_id = message_id;
    }

    public String getMessage_id() {
        return message_id;
    }

    public void setMessage_id(String message_id) {
        this.message_id = message_id;
    }
}
