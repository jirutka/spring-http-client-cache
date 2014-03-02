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

import java.lang.ref.SoftReference;

@ThreadSafe
public class SoftReferenceSynchronizedLruCache extends SynchronizedLruCache {

    /**
     * Create a new instance with default initial capacity and load factor.
     *
     * @param name An arbitrary name of this cache instance.
     * @param capacity The maximal capacity.
     */
    public SoftReferenceSynchronizedLruCache(String name, int capacity) {
        super(name, capacity);
    }

    public SoftReferenceSynchronizedLruCache(String name, int capacity, int initialCapacity, float loadFactory) {
        super(name, capacity, initialCapacity, loadFactory);
    }


    @Override
    public synchronized ValueWrapper get(Object key) {
        ValueWrapper wrapped = super.get(key);

        if (wrapped != null && wrapped.get() == null) {
            // remove entry from cache if it's not valid, perhaps removed by GC
            evict(key);
        }
        return wrapped;
    }

    @Override
    protected ValueWrapper createEntry(Object value) {
        return new SoftReferenceWrapper(value);
    }


    static class SoftReferenceWrapper implements ValueWrapper {

        private final SoftReference<Object> value;

        SoftReferenceWrapper(Object value) {
            this.value = new SoftReference<>(value);
        }

        public Object get() {
            return value.get();
        }
    }
}
