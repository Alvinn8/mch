package ca.bkaw.mch.repository;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An object that contains profiles.
 *
 * @param <T> The type of profile.
 */
public class Profiles<T> {
    private final Map<String, T> data;

    public Profiles() {
        this(new HashMap<>());
    }

    public Profiles(Map<String, T> data) {
        this.data = data;
    }

    /**
     * Get an <a href="Collection.html#unmodview">unmodifiable view</a> of the profiles
     * and their names.
     *
     * @return The unmodifiable map.
     */
    public Map<String, T> getProfiles() {
        return Collections.unmodifiableMap(this.data);
    }

    /**
     * Get a profile by name.
     *
     * @param name The name of the profile.
     * @return The profile, or null.
     */
    @Nullable
    public T getProfile(@NotNull String name) {
        return this.data.get(name);
    }

    /**
     * Set a profile by name.
     *
     * @param name The name.
     * @param profile The profile.
     */
    public void setProfile(@NotNull String name, @Nullable T profile) {
        this.data.put(name, profile);
    }

    /**
     * Remove a profile by name, if it exists.
     *
     * @param name The profile to remove.
     */
    public void remove(@NotNull String name) {
        this.data.remove(name);
    }
}
