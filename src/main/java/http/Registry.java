package http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import model.History;
import model.Image;
import model.Layer;
import model.Manifest;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Registry {

    private OkHttpClient client;
    private final String registry;
    private String basicAuth = null;

    public Registry(String registryHostname) {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        this.registry = registryHostname;
    }

    public List<String> getRepositories() throws IOException {
        Request request = getRequestBuilder()
                .url("https://" + registry + "/v2/_catalog")
                .get()
                .build();

        ArrayList<String> repositories = new ArrayList<>();
        Response response = client.newCall(request).execute();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response.body().string());
        for (JsonNode repo : jsonNode.get("repositories")) {
            repositories.add(repo.asText());
        }
        return repositories;
    }

    public List<String> getTags(String repository) throws IOException {
        Request request = getRequestBuilder()
                .url("https://" + registry + "/v2/" + repository + "/tags/list")
                .get()
                .build();

        ArrayList<String> tags = new ArrayList<>();
        Response response = client.newCall(request).execute();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(response.body().string());
        for (JsonNode tag : jsonNode.get("tags")) {
            tags.add(tag.asText());
        }
        return tags;
    }

    public Manifest getManifest(Image image) throws IOException {
        Request request = getRequestBuilder()
                .url("https://" + registry + "/v2/" + image.getRepository() + "/manifests/" + image.getTag())
                .addHeader("accept", "application/vnd.docker.distribution.manifest.v2+json")
                .get()
                .build();

        Response response = client.newCall(request).execute();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode manifestJsonNode = mapper.readTree(response.body().string());

        Manifest manifest = new Manifest(image);
        manifest.setImageDigest(manifestJsonNode.get("config").get("digest").asText());
        manifest.setManifestDigest(response.header("docker-content-digest"));


        for (JsonNode layerJson : manifestJsonNode.get("layers")) {
            manifest.addLayer(new Layer(
                    layerJson.get("digest").asText(),
                    layerJson.get("size").asInt(),
                    image.getRepository(),
                    manifest.getImageDigest()
            ));
        }

        request = getRequestBuilder()
                .url("https://" + registry + "/v2/" + image.getRepository() + "/blobs/" + manifest.getImageDigest())
                .get()
                .build();

        response = client.newCall(request).execute();
        manifestJsonNode = mapper.readTree(response.body().string());
        JsonNode container = manifestJsonNode.get("container");
        manifest.setContainer(container == null ? null : container.asText());
        JsonNode dockerVersion = manifestJsonNode.get("docker_version");
        manifest.setDockerVersion(dockerVersion == null ? null : dockerVersion.asText());
        manifest.setCreated(ZonedDateTime.parse(manifestJsonNode.get("created").asText()));

        int i = 0;
        for (JsonNode historyJson : manifestJsonNode.get("history")) {

            boolean isEmptyLayer = historyJson.get("empty_layer") != null && historyJson.get("empty_layer").asBoolean();
            manifest.addHistory(new History(
                    ZonedDateTime.parse(historyJson.get("created").asText()),
                    historyJson.get("created_by") == null ? null : historyJson.get("created_by").asText(),
                    isEmptyLayer,
                    isEmptyLayer ? null : manifest.getLayerList().get(i).getDigest()
            ));
            i += isEmptyLayer ? 0 : 1;
        }

        return manifest;
    }

    public void setLayerLastModified(Layer layer) throws IOException {
        Request request = getRequestBuilder()
                .url("https://" + registry + "/v2/" + layer.getRepository() + "/blobs/" + layer.getDigest())
                .head()
                .build();

        Response response = client.newCall(request).execute();

        String lastModified = response.header("Last-Modified");

        if (lastModified != null) {
            layer.setLastModified(ZonedDateTime.parse(lastModified, DateTimeFormatter.RFC_1123_DATE_TIME));
        }

    }

    public void setBasicAuth(String basicAuth) {
        this.basicAuth = basicAuth;
    }

    private Request.Builder getRequestBuilder() {
        Request.Builder builder = new Request.Builder();

        if (basicAuth != null) {
            builder.header("Authorization", "Basic " + basicAuth);
        }
        return builder;
    }
}
