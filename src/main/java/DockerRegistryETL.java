import data.Database;
import http.Registry;
import io.reactivex.rxjava3.core.Flowable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import model.History;
import model.Image;
import picocli.CommandLine;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;


@CommandLine.Command(description = "Dumps data from a docker registry in a sqlite database generated file.",
        name = "docker-registry-etl", mixinStandardHelpOptions = true)
public class DockerRegistryETL implements Callable<Integer> {

    @CommandLine.Option(
            names = {"-r", "--registry"},
            description = "Hostname for the docker registry (without http[s]://)",
            required = true
    )
    private String registryHost;

    @CommandLine.Option(
            names = {"-t", "--threads"},
            description = "Number of concurrent threads to be used",
            defaultValue = "20",
            required = false
    )
    private int numThreads;

    @CommandLine.Option(
            names = {"-i", "--images"},
            paramLabel = "FILE",
            description = "File containing a list of images to be scanned, with the format 'image:tag' on each line"
    )
    private File fileImages;

    @CommandLine.Option(
            names = {"--basic-auth"},
            description = "Base64 encoded 'username:password' String",
            required = false
    )
    private String basicAuth;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new DockerRegistryETL()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        List<Image> images = new ArrayList<>();

        Registry registry = new Registry(registryHost);
        if (basicAuth != null) {
            registry.setBasicAuth(basicAuth);
        }
        Database database = new Database();

        database.createDatabase();

        List<String> repositories = registry.getRepositories();

        if (fileImages != null) {
            List<String> lines = Files.readAllLines(fileImages.toPath());
            for (String line : lines) {
                String[] imageTag = line.split(":");
                images.add(new Image(imageTag[0], imageTag[1]));
            }
        } else {
            images = Flowable.fromIterable(repositories)
                    .parallel(numThreads)
                    .runOn(Schedulers.io())
                    .flatMap(repository -> {
                        ArrayList<Image> imagesList = new ArrayList<>();
                        List<String> tags = registry.getTags(repository);

                        for (String tag : tags) {
                            imagesList.add(new Image(repository, tag));
                        }
                        return Flowable.fromIterable(imagesList);
                    })
                    .sequential()
                    .toList()
                    .blockingGet();
        }

        Flowable.fromIterable(images)
                .parallel(numThreads)
                .runOn(Schedulers.io())
                .map(image -> {
                    System.out.println(image.getRepository() + ":" + image.getTag() + " loading");
                    return registry.getManifest(image);
                })
                .flatMap(manifest -> {
                    database.insertManifest(manifest);

                    for (History history : manifest.getHistoryList()) {
                        database.insertHistory(history, manifest.getImageDigest());
                    }
                    database.commit();

                    return Flowable.fromIterable(manifest.getLayerList());
                })
                .map(layer -> {
                    registry.setLayerLastModified(layer);
                    database.insertLayer(layer);
                    database.commit();

                    return new Object();
                })
                .sequential()
                .blockingSubscribe();
        return 0;
    }
}
