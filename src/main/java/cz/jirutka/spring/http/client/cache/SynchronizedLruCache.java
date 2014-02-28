/*
 * Copyright 2014 Jakub Jirutka <jakub@jirutka.cz>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cz.jirutka.spring.http.client.cache;

import net.jcip.annotations.ThreadSafe;
import org.springframework.cache.Cache;
import org.springframework.cache.support.SimpleValueWrapper;
import org.springframework.util.Assert;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Simple LRU {@link Cache} implementation with adjustable capacity limit
 * based on {@link LinkedHashMap} from JDK.
 */
@ThreadSafe
public class SynchronizedLruCache implements Cache {

    public static final int DEFAULT_INITIAL_CAPACITY = 16;

    public static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private final String name;

    private final int capacity;

    private final LinkedHashMap<Object, ValueWrapper> store;


    /**
     * Create a new instance with default initial capacity and load factor.
     *
     * @param name An arbitrary name of this cache instance.
     * @param capacity The maximal capacity.
     */
    public SynchronizedLruCache(String name, int capacity) {
        this(name, capacity, (DEFAULT_INITIAL_CAPACITY > capacity ? capacity : DEFAULT_INITIAL_CAPACITY), DEFAULT_LOAD_FACTOR);
    }

    public SynchronizedLruCache(String name, int capacity, int initialCapacity, float loadFactory) {
        Assert.hasText(name, "name should not be blank");
        Assert.isTrue(capacity > 0, "capacity must be greater then 0");

        this.name = name;
        this.capacity = capacity;

        this.store = new LinkedHashMap<Object, ValueWrapper>(initialCapacity, loadFactory, true) {
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return this.size() > SynchronizedLruCache.this.capacity;
            }
        };
    }


    public String getName() {
        return name;
    }

    public LinkedHashMap getNativeCache() {
        return store;
    }

    public synchronized ValueWrapper get(Object key) {
        return store.get(key);
    }

    public synchronized void put(Object key, Object value) {
        store.put(key, createEntry(value));
    }

    public synchronized void evict(Object key) {
        store.remove(key);
    }

    public synchronized void clear() {
        store.clear();
    }


    protected ValueWrapper createEntry(Object value) {
        return new SimpleValueWrapper(value);
    }
}
