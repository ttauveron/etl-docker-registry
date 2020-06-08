package model;

import java.time.ZonedDateTime;

public class History {
    private String layerDigest = null;
    private ZonedDateTime created;
    private String createdBy;
    private boolean emptyLayer;

    public History(ZonedDateTime created, String createdBy, Boolean emptyLayer, String layerDigest) {
        this.created = created;
        this.createdBy = createdBy;
        this.emptyLayer = emptyLayer == null ? false : emptyLayer;
        this.layerDigest = layerDigest;
    }

    public boolean isEmptyLayer() {
        return emptyLayer;
    }

    public String getLayerDigest() {
        return layerDigest;
    }

    public void setLayerDigest(String layerDigest) {
        this.layerDigest = layerDigest;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
