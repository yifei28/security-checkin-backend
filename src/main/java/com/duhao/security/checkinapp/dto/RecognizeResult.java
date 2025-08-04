package com.duhao.security.checkinapp.dto;

public class RecognizeResult {
    private boolean success;
    private String guardId;
    private double similarity;
    private String message;

    // getters & setters

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getGuardId() { return guardId; }
    public void setGuardId(String guardId) { this.guardId = guardId; }

    public double getSimilarity() { return similarity; }
    public void setSimilarity(double similarity) { this.similarity = similarity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
