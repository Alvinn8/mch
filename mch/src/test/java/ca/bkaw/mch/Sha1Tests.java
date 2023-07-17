package ca.bkaw.mch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class Sha1Tests {
    @Test
    public void sha1Test() throws URISyntaxException, IOException {
        URL url = Sha1Tests.class.getClassLoader().getResource("file1.txt");
        assertNotNull(url);
        Path path = Path.of(url.toURI());

        Sha1 fileSha1 = Sha1.ofFile(path);
        Sha1 sha1 = Sha1.fromString("943a702d06f34599aee1f8da8ef9f7296031d699");

        assertEquals(fileSha1, sha1);
    }
}
