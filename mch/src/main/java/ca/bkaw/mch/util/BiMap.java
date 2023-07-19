package ca.bkaw.mch.util;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * A map that also stores a reverse version of the map.
 *
 * @param <K> The key type.
 * @param <V> The value type.
 */
public class BiMap<K, V> extends HashMap<K, V> {
    private final BiMap<V, K> reverse;

    public BiMap() {
        this.reverse = new BiMap<>(this);
    }

    public BiMap(int initialCapacity) {
        super(initialCapacity);
        this.reverse = new BiMap<>(this, initialCapacity);
    }

    private BiMap(BiMap<V, K> reverse) {
        this.reverse = reverse;
    }

    private BiMap(BiMap<V, K> reverse, int initialCapacity) {
        super(initialCapacity);
        this.reverse = reverse;
    }

    /**
     * Get the reverse view of this BiMap.
     *
     * @return The reverse view.
     */
    public BiMap<V, K> reverse() {
        return this.reverse;
    }

    @Override
    public V put(K key, V value) {
        V ret = super.put(key, value);
        this.reverse.putDirect(value, key);
        return ret;
    }

    private void putDirect(K key, V value) {
        super.put(key, value);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V computeIfPresent(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V compute(K key, BiFunction<? super K, ? super V, ? extends V> remappingFunction) {
        throw new UnsupportedOperationException();
    }

    @Override
    public V putIfAbsent(K key, V value) {
        throw new UnsupportedOperationException();
    }
}
