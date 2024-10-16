package ca.bkaw.mch.repository.remote;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.repository.DimensionAccess;
import ca.bkaw.mch.util.StringPath;
import ca.bkaw.mch.util.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class RemoteDimensionAccess implements DimensionAccess {
    private final URL baseUrl;
    private final String authorization;
    private final Sha1 commitSha1;
    private final Sha1 worldSha1;
    private final String dimensionKey;

    public RemoteDimensionAccess(URL baseUrl, String authorization, Sha1 commitSha1, Sha1 worldSha1, String dimensionKey) {
        this.baseUrl = baseUrl;
        this.authorization = authorization;
        this.commitSha1 = commitSha1;
        this.worldSha1 = worldSha1;
        this.dimensionKey = dimensionKey;
    }

    private DataInputStream sendRequest(String endpoint, StringPath path) throws IOException {
        String urlStr = Util.trailingSlash(this.baseUrl.toString()) + Util.noLeadingSlash(endpoint);
        URL url = URI.create(urlStr).toURL();

        System.out.println("sending to " + url);
        JSONObject body = new JSONObject();
        body.put("commit", this.commitSha1.asHex());
        body.put("world", this.worldSha1.asHex());
        body.put("dimension", this.dimensionKey);
        body.put("path", path.toString());

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", this.authorization);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);
        connection.getOutputStream().write(body.toString().getBytes(StandardCharsets.UTF_8));
        connection.getOutputStream().close();
        connection.connect();
        if (connection.getResponseCode() == 404) {
            return null; // TODO stop abusing HTTP spec
        }
        if (connection.getResponseCode() != 200) {
            throw new IOException("Non 200 status code: HTTP " + connection.getResponseCode());
        }
        return new DataInputStream(connection.getInputStream());
    }

    @Override
    public @Nullable InputStream restoreFile(StringPath path) throws IOException {
        return this.sendRequest("v1/restoreFile", path);
    }

    @Override
    public @NotNull List<String> list(StringPath path) throws IOException {
        try (DataInputStream stream = this.sendRequest("v1/restoreFile", path)) {
            JSONObject json = new JSONObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            JSONArray array = json.getJSONArray("value");
            List<String> list = new ArrayList<>(array.length());
            for (int i = 0; i < array.length(); i++) {
                list.add(array.getString(i));
            }
            return list;
        }
    }
}
