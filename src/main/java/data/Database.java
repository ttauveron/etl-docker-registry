package data;

import model.History;
import model.Layer;
import model.Manifest;
import org.apache.ibatis.jdbc.ScriptRunner;
import org.sqlite.SQLiteErrorCode;
import org.sqlite.SQLiteException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.format.DateTimeFormatter;

public class Database {

    private Connection localDBConnection = null;

    public synchronized void commit() throws SQLException {
        localDBConnection.commit();
    }

    public Database() {
        try {
            Class.forName("org.sqlite.JDBC");
            localDBConnection = DriverManager.getConnection("jdbc:sqlite:docker-registry.db");
            localDBConnection.setAutoCommit(false);
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void insertManifest(Manifest manifest) throws SQLException {
        String manifestInsertSql = "INSERT INTO manifests (image_digest,container,created,docker_version,manifest_digest) VALUES (?,?,?,?,?)";
        PreparedStatement pstmt = localDBConnection.prepareStatement(manifestInsertSql);
        pstmt.setString(1, manifest.getImageDigest());
        pstmt.setString(2, manifest.getContainer());
        pstmt.setString(3, manifest.getCreated().format(DateTimeFormatter.ISO_DATE_TIME));
        pstmt.setString(4, manifest.getDockerVersion());
        pstmt.setString(5, manifest.getManifestDigest());
        continueIfExists(pstmt);

        String imageInsertSql = "INSERT INTO images (name,tag,digest) VALUES (?,?,?)";
        pstmt = localDBConnection.prepareStatement(imageInsertSql);
        pstmt.setString(1, manifest.getImage().getRepository());
        pstmt.setString(2, manifest.getImage().getTag());
        pstmt.setString(3, manifest.getImageDigest());
        pstmt.execute();
    }

    public void insertHistory(History history, String imageDigest) throws SQLException {
        String historyInsertSql = "INSERT INTO history (layer_digest,created,created_by,image_digest) VALUES (?,?,?,?)";
        PreparedStatement pstmt = localDBConnection.prepareStatement(historyInsertSql);

        pstmt.setString(1, history.getLayerDigest());
        pstmt.setString(2, history.getCreated().format(DateTimeFormatter.ISO_DATE_TIME));
        pstmt.setString(3, history.getCreatedBy());
        pstmt.setString(4, imageDigest);
        pstmt.execute();
    }

    public void insertLayer(Layer layer) throws SQLException {
        String layersInsertSql = "INSERT INTO layers (digest,size,last_modified) VALUES (?,?,?)";
        PreparedStatement pstmt = localDBConnection.prepareStatement(layersInsertSql);

        pstmt.setString(1, layer.getDigest());
        pstmt.setInt(2, layer.getSize());

        String lastModified = null;
        if (layer.getLastModified() != null) {
            lastModified = layer.getLastModified().format(DateTimeFormatter.ISO_DATE_TIME);
        }
        pstmt.setString(3, lastModified);
        continueIfExists(pstmt);

        String layerManifestInsertSql = "INSERT INTO layer_manifest (layer_digest,image_digest) VALUES (?,?)";
        pstmt = localDBConnection.prepareStatement(layerManifestInsertSql);
        pstmt.setString(1, layer.getDigest());
        pstmt.setString(2, layer.getImageDigest());
        continueIfExists(pstmt);
    }

    private void continueIfExists(PreparedStatement pstmt) throws SQLException {
        try {
            pstmt.execute();
        } catch (SQLiteException e) {
            if (e.getResultCode() != SQLiteErrorCode.SQLITE_CONSTRAINT) {
                throw e;
            }
        }
    }

    public void createDatabase() throws SQLException {
        ScriptRunner sr = new ScriptRunner(localDBConnection);

        BufferedReader reader = new BufferedReader(new InputStreamReader(Database.class.getClassLoader().getResourceAsStream("create.sql")));
        sr.setEscapeProcessing(false);
        sr.setLogWriter(null);
        sr.runScript(reader);
        commit();
    }


}