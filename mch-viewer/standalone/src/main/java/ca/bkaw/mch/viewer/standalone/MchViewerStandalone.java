package ca.bkaw.mch.viewer.standalone;

import java.io.IOException;

public class MchViewerStandalone {
    public static void main(String[] args) {
        try {
            new Server(25565).start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
