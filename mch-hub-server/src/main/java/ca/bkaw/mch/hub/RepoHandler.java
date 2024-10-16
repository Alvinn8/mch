package ca.bkaw.mch.hub;

import ca.bkaw.mch.Sha1;
import ca.bkaw.mch.object.Reference20;
import ca.bkaw.mch.object.commit.Commit;
import ca.bkaw.mch.repository.DimensionAccess;
import ca.bkaw.mch.repository.MchRepository;
import ca.bkaw.mch.util.StringPath;
import com.sun.net.httpserver.HttpExchange;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class RepoHandler {
    private final MchRepository repository;

    public RepoHandler(MchRepository repository) {
        this.repository = repository;
    }

    public void getHeadCommit(HttpExchange exchange) throws IOException {
        try {
            System.out.println("heyo");
            Reference20<Commit> headCommit = repository.getHeadCommit();
            // TODO handle when null
            exchange.sendResponseHeaders(200, 20);
            exchange.getResponseBody().write(headCommit.getSha1().getBytes());
            exchange.close();
            System.out.println("byeo");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private JSONObject readBody(HttpExchange exchange) throws IOException {
        String str = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return new JSONObject(str);
    }

    public void getTrackedWorlds(HttpExchange exchange) throws IOException {
        System.out.println("Hello!");
        List<Sha1> worlds = this.repository.getTrackedWorlds();
        exchange.getRequestBody().close();
        exchange.sendResponseHeaders(200, 4 + 20L * worlds.size());

        DataOutputStream stream = new DataOutputStream(exchange.getResponseBody());
        stream.writeInt(worlds.size());
        for (Sha1 sha1 : worlds) {
            stream.write(sha1.getBytes());
        }
        stream.close();
        exchange.close();
    }

    public void getTrackedWorld(HttpExchange exchange) throws IOException {
        JSONObject body = this.readBody(exchange);
        String name = body.getString("name");
        Sha1 trackedWorld = this.repository.getTrackedWorld(name);
        exchange.sendResponseHeaders(200, 20);
        // TODO handle null
        exchange.getResponseBody().write(trackedWorld.getBytes());
        exchange.getResponseBody().close();
    }

    public void getDimensions(HttpExchange exchange) throws IOException {
        JSONObject body = this.readBody(exchange);
        Sha1 commitSha1 = Sha1.fromString(body.getString("commit"));
        Sha1 worldSha1 = Sha1.fromString(body.getString("world"));
        List<String> dimensions = this.repository.getDimensions(commitSha1, worldSha1);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        dos.writeInt(dimensions.size());
        for (String dimension : dimensions) {
            dos.writeUTF(dimension);
        }

        exchange.sendResponseHeaders(200, baos.size());
        exchange.getResponseBody().write(baos.toByteArray());
        exchange.getResponseBody().close();
    }

    public void accessCommit(HttpExchange exchange) throws IOException {
        try {
            JSONObject body = this.readBody(exchange);
            Sha1 commitSha1 = Sha1.fromString(body.getString("commit"));
            Commit commit = this.repository.accessCommit(commitSha1);

            // TODO do this in a smarter way to avoid deserializing to instantly serialize.
            //  Make a way to stream objects instead maybe.
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            commit.write(dos);

            exchange.sendResponseHeaders(200, baos.size());
            exchange.getResponseBody().write(baos.toByteArray());
            exchange.getResponseBody().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void restoreFile(HttpExchange exchange) throws IOException {
        try {
            JSONObject body = this.readBody(exchange);
            Sha1 commitSha1 = Sha1.fromString(body.getString("commit"));
            Sha1 worldSha1 = Sha1.fromString(body.getString("world"));
            String dimensionKey = body.getString("dimension");
            StringPath path = StringPath.of(body.getString("path"));

            DimensionAccess dimensionAccess = this.repository.accessDimension(commitSha1, worldSha1, dimensionKey);
            InputStream stream = dimensionAccess != null ? dimensionAccess.restoreFile(path) : null;
            if (stream == null) {
                // TODO this is abuse of the HTTP spec
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            // TODO make restoreFile take a path instead maybe? We are writing to and from
            //  streams and files so many times unnecessarily right now due to the abstraction.

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            stream.transferTo(baos);

            exchange.sendResponseHeaders(200, baos.size());
            exchange.getResponseBody().write(baos.toByteArray());
            exchange.getResponseBody().close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void list(HttpExchange exchange) throws IOException {
        JSONObject body = this.readBody(exchange);
        Sha1 commitSha1 = Sha1.fromString(body.getString("commit"));
        Sha1 worldSha1 = Sha1.fromString(body.getString("world"));
        String dimensionKey = body.getString("dimension");
        StringPath path = StringPath.of(body.getString("path"));

        DimensionAccess dimensionAccess = this.repository.accessDimension(commitSha1, worldSha1, dimensionKey);
        List<String> list = dimensionAccess.list(path);
        JSONArray array = new JSONArray(list);

        byte[] bytes = array.toString().getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.getResponseBody().close();
    }
}
