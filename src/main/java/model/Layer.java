package model;

import java.time.ZonedDateTime;

public class Layer {
    private String digest;
    private String imageDigest;
    private int size;
    private ZonedDateTime lastModified;
    private String repository;

    public Layer(String digest, int size, String repository, String imageDigest) {
        this.digest = digest;
        this.size = size;
        this.repository = repository;
        this.imageDigest = imageDigest;
    }

    public String getDigest() {
        return digest;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public void setRepository(String repository) {
        this.repository = repository;
    }

    public String getRepository() {
        return repository;
    }

    public void setDigest(String digest) {
        this.digest = digest;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public ZonedDateTime getLastModified() {
        return lastModified;
    }

    public void setLastModified(ZonedDateTime lastModified) {
        this.lastModified = lastModified;
    }
}
