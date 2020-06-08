package model;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

public class Manifest {
    private Image image;
    private String imageDigest;
    private String container;
    private String manifestDigest;
    private ZonedDateTime created;
    private String dockerVersion;
    private List<History> historyList = new ArrayList<>();
    private List<Layer> layerList = new ArrayList<>();

    public Manifest(Image image) {
        this.image = image;
    }

    public Image getImage() {
        return image;
    }

    public void addHistory(History history) {
        this.historyList.add(history);
    }

    public void addLayer(Layer layer) {
        this.layerList.add(layer);
    }

    public List<History> getHistoryList() {
        return historyList;
    }

    public List<Layer> getLayerList() {
        return layerList;
    }

    public void setImageDigest(String imageDigest) {
        this.imageDigest = imageDigest;
        this.image.setDigest(imageDigest);
    }

    public String getManifestDigest() {
        return manifestDigest;
    }

    public void setManifestDigest(String manifestDigest) {
        this.manifestDigest = manifestDigest;
    }

    public void setContainer(String container) {
        this.container = container;
    }

    public void setCreated(ZonedDateTime created) {
        this.created = created;
    }

    public void setDockerVersion(String dockerVersion) {
        this.dockerVersion = dockerVersion;
    }

    public String getImageDigest() {
        return imageDigest;
    }

    public String getContainer() {
        return container;
    }

    public ZonedDateTime getCreated() {
        return created;
    }

    public String getDockerVersion() {
        return dockerVersion;
    }
}
