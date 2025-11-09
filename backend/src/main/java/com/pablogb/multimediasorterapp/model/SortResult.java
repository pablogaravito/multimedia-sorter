package com.pablogb.multimediasorterapp.model;

public class SortResult {
    private boolean success;
    private String message;
    private int copied;
    private int skipped;
    private int failed;

    public SortResult() {}

    public SortResult(boolean success, String message, int copied, int skipped, int failed) {
        this.success = success;
        this.message = message;
        this.copied = copied;
        this.skipped = skipped;
        this.failed = failed;
    }

    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public int getCopied() { return copied; }
    public void setCopied(int copied) { this.copied = copied; }

    public int getSkipped() { return skipped; }
    public void setSkipped(int skipped) { this.skipped = skipped; }

    public int getFailed() { return failed; }
    public void setFailed(int failed) { this.failed = failed; }
}
