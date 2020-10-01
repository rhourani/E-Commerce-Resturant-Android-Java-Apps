package com.ds.androideatitv2client.Model;

public class  BraintreeTransaction {
    private boolean success;
    private Transaction transaction;

    public BraintreeTransaction() {
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }
}
