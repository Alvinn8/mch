package ca.bkaw.mch.repository.remote;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.ObjectStorageTypes;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.repository.DimensionAccess;
import ca.bkaw.mch.repository.RepositoryAccess;
import ca.bkaw.mch.util.Util;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class RemoteRepositoryAccess implements RepositoryAccess {
    private final URL baseUrl;
    private final String authorization;

    public RemoteRepositoryAccess(URL baseUrl, String authorization) {
        this.baseUrl = baseUrl;
        this.authorization = authorization;
    }

    private DataInputStream sendRequest(String endpoint) throws IOException {
        String urlStr = Util.trailingSlash(this.baseUrl.toString()) + Util.noLeadingSlash(endpoint);
        URL url = URI.create(urlStr).toURL();

        // System.out.println("sending to " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Authorization", this.authorization);
        connection.connect();
        if (connection.getResponseCode() != 200) {
            throw new IOException("Non 200 status code: HTTP " + connection.getResponseCode());
        }
        return new DataInputStream(connection.getInputStream());
    }

    private DataInputStream sendRequest(String endpoint, JSONObject body) throws IOException {
        String urlStr = Util.trailingSlash(this.baseUrl.toString()) + Util.noLeadingSlash(endpoint);
        URL url = URI.create(urlStr).toURL();

        // System.out.println("sending to " + url);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", this.authorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
        connection.getOutputStream().close();
        connection.connect();
        if (connection.getResponseCode() != 200) {
            throw new IOException("Non 200 status code: HTTP " + connection.getResponseCode());
        }
        return new DataInputStream(connection.getInputStream());
    }

    @Override
    public @Nullable Reference20<Commit> getHeadCommit() throws IOException {
        try (DataInputStream stream = this.sendRequest("v1/getHeadCommit")) {
            return Reference20.read(stream, ObjectStorageTypes.COMMIT);
        }
    }

    @Override
    public List<Sha1> getTrackedWorlds() throws IOException {
        try (DataInputStream stream = this.sendRequest("v1/getTrackedWorlds")) {
            int length = stream.readInt();
            List<Sha1> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(Sha1.read(stream));
            }
            return list;
        }
    }

    @Override
    public @Nullable Sha1 getTrackedWorld(String name) throws IOException {
        JSONObject body = new JSONObject(Map.of("name", name));
        try (DataInputStream stream = this.sendRequest("v1/getTrackedWorld", body)) {
            return Sha1.read(stream);
        }
    }

    @Override
    public List<String> getDimensions(Sha1 commitSha1, Sha1 worldSha1) throws IOException {
        JSONObject body = new JSONObject(Map.of(
            "commit", commitSha1.asHex(),
            "world", worldSha1.asHex()
        ));
        try (DataInputStream stream = this.sendRequest("v1/getDimensions", body)) {
            int length = stream.readInt();
            List<String> list = new ArrayList<>(length);
            for (int i = 0; i < length; i++) {
                list.add(stream.readUTF());
            }
            return list;
        }
    }

    @Override
    public Commit accessCommit(Sha1 commitSha1) throws IOException {
        JSONObject body = new JSONObject(Map.of("commit", commitSha1.asHex()));
        try (DataInputStream stream = this.sendRequest("v1/accessCommit", body)) {
            return new Commit(stream);
        }
    }

    @Override
    public @Nullable DimensionAccess accessDimension(Sha1 commitSha1, Sha1 worldSha1, String dimensionKey) throws IOException {
        return new RemoteDimensionAccess(
            this.baseUrl, this.authorization, commitSha1, worldSha1, dimensionKey
        );
    }
}
