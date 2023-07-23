package ca.bkaw.mch.world;

import java.util.List;

/**
 * An object that provides mch with information about a world.
 */
public interface WorldProvider {
    List<String> getDimensions();
}
