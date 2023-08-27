package ca.bkaw.mch.world;

import ca.bkaw.mch.repository.MchRepository;

import java.io.DataOutput;
import java.io.IOException;

/**
 * An object that stores information on how a world can be accessed.
 * <p>
 * To access the world, use {@link #access(MchRepository)}, which in some cases may connect to
 * external file servers.
 */
public interface WorldAccessor {
    /**
     * Access the world.
     *
     * @return The {@link WorldProvider}.
     * @throws IOException If an I/O error occurs.
     */
    WorldProvider access(MchRepository mchRepository) throws IOException;

    /**
     * Get the id of this world accessor.
     *
     * @return The id.
     */
    byte getId();

    /**
     * Write the world accessor to the configuration.
     *
     * @param dataOutput The data output to write to.
     * @throws IOException If an I/O error occurs.
     */
    void write(DataOutput dataOutput) throws IOException;
}
