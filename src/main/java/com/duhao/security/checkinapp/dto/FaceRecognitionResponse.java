package com.duhao.security.checkinapp.dto;

public class FaceRecognitionResponse {
    private boolean success;
    private String message;

    public FaceRecognitionResponse() {}

    public FaceRecognitionResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static FaceRecognitionResponse ok() {
        return new FaceRecognitionResponse(true, "识别通过");
    }

    public static FaceRecognitionResponse fail(String msg) {
        return new FaceRecognitionResponse(false, msg);
    }

    // getters & setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
