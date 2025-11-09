package com.pablogb.multimediasorterapp.model;

public class PathValidation {
    private boolean exists;
    private boolean isDirectory;
    private boolean isReadable;
    private boolean isWritable;

    public PathValidation() {}

    public PathValidation(boolean exists, boolean isDirectory, boolean isReadable, boolean isWritable) {
        this.exists = exists;
        this.isDirectory = isDirectory;
        this.isReadable = isReadable;
        this.isWritable = isWritable;
    }

    public boolean isExists() { return exists; }
    public void setExists(boolean exists) { this.exists = exists; }

    public boolean isDirectory() { return isDirectory; }
    public void setDirectory(boolean directory) { isDirectory = directory; }

    public boolean isReadable() { return isReadable; }
    public void setReadable(boolean readable) { isReadable = readable; }

    public boolean isWritable() { return isWritable; }
    public void setWritable(boolean writable) { isWritable = writable; }
}
