package pipingj;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Force to release the Url Key after 5 minutes.
 * If sender and receiver already connected, this should not be impacted.
 *
 * @param <K>
 * @param <V>
 */
public class TimedConcurrentHashMap<K, V> {
    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    V computeIfAbsent(K key, Function<? super K, ? extends V> mappingFunction) {
        V v = map.computeIfAbsent(key, mappingFunction);
        scheduler.schedule(() -> map.remove(key), 5, TimeUnit.MINUTES);
        return v;
    }

    V get(K key) {
        return map.get(key);
    }

    void remove(K key) {
        map.remove(key);
    }

    boolean containsKey(K key) {
        return map.containsKey(key);
    }
}




