package ca.bkaw.mch.cli.hub;

import ca.bkaw.mch.hub.MchHub;
import ca.bkaw.mch.hub.MchHubServer;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Parameters;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.concurrent.CountDownLatch;

@Command(name = "hub")
public class HubCommand {
    @Command(name = "start")
    public int start(
        @Parameters(index = "0", defaultValue = "0.0.0.0")
        InetAddress host,
        @Parameters(index = "1", defaultValue = "4148")
        int port
    ) {
        try {
            MchHubServer server = MchHub.start(new InetSocketAddress(host, port));
            if (server == null) {
                return ExitCode.USAGE;
            }

            CountDownLatch latch = new CountDownLatch(1);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    System.out.println("\nStopping mch-hub server");
                    server.stop(0);
                } finally {
                    latch.countDown();
                }
            }));

            latch.await();

            return ExitCode.OK;
        } catch (Throwable e) {
            System.err.println("mch-hub-server crashed");
            e.printStackTrace();
            return ExitCode.SOFTWARE;
        }
    }
}
