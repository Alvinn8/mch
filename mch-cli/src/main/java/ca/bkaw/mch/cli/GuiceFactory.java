package ca.bkaw.mch.cli;

import ca.bkaw.mch.repository.MchRepository;
import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IFactory;

import javax.inject.Qualifier;
import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.file.Files;
import java.nio.file.Path;

public class GuiceFactory implements IFactory {
    @Qualifier
    @Retention(RetentionPolicy.RUNTIME)
    public @interface WorkingDir {}

    private final Injector injector;

    public GuiceFactory(Path workingDir) {
        this.injector = Guice.createInjector(new DemoModule(workingDir));
    }

    @Override
    public <K> K create(Class<K> cls) throws Exception {
        try {
            return this.injector.getInstance(cls);
        } catch (ConfigurationException ex) {
            return CommandLine.defaultFactory().create(cls);
        }
    }

    static class DemoModule extends AbstractModule {
        private final Path workingDir;

        DemoModule(Path workingDir) {
            this.workingDir = workingDir;
        }

        @Provides
        @WorkingDir
        private Path getWorkingDir() {
            return this.workingDir;
        }

        @Provides
        private MchRepository findRepository() {
            MchRepository mchRepository = this.tryFindRepositoryInPath(this.workingDir.toAbsolutePath());
            if (mchRepository == null) {
                // This should probably be an exception that propagates up and is printed nicely.
                // For now, we print and system.exit.
                System.err.println("No mch repository found in this directory.");
                System.exit(ExitCode.USAGE);
            }
            try {
                mchRepository.readConfiguration();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read mch configuration", e);
            }
            return mchRepository;
        }

        private MchRepository tryFindRepositoryInPath(Path path) {
            Path mchPath = path.resolve("mch");
            if (Files.isDirectory(mchPath)) {
                return new MchRepository(mchPath);
            }
            return null;
        }

    }
}
