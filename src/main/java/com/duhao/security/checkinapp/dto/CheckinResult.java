package com.duhao.security.checkinapp.dto;

public class CheckinResult {
    private boolean success;
    private String message;

    public CheckinResult() {}
    public CheckinResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    public static CheckinResult ok() {
        return new CheckinResult(true, null);
    }
    public static CheckinResult fail(String msg) {
        return new CheckinResult(false, msg);
    }
    // getters & setters...
    public boolean isSuccess() {
        return success;
    }
    public void setSuccess(boolean success) {
        this.success = success;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
}