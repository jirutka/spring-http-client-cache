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
package cz.jirutka.spring.http.client.cache

import org.springframework.cache.Cache
import spock.lang.Specification

abstract class BaseLruCacheTest extends Specification {

    def cache = createCache(16)

    abstract Cache createCache(int capacity)


    void setup() {
        seed cache
    }

    def 'construct with illegal capacity'() {
        when:
            createCache(capacity)
        then:
            thrown IllegalArgumentException
        where:
            capacity << [0, -6]
    }

    def 'get: should return null when entry does not exist'() {
        expect:
           cache.get('non-existing-bullshit') == null
    }

    def 'put/get: should return cached entry'() {
        setup:
            def (key, value) = ['bad', 'wolf']
        and:
            assert ! cache.get(key)
        when:
            cache.put(key, value)
        then:
            cache.get(key).get() == value
    }

    def 'put: does not accept null value'() {
        when:
            cache.put('wrong', null)
        then:
            thrown IllegalArgumentException
    }

    def 'evict: should remove existing entry'() {
        setup:
            cache.put('dead', 'wolf')
        and:
            assert cache.get('dead')
        when:
            cache.evict('dead')
        then:
            cache.get('dead') == null
    }

    def 'clear: should remove all entries'() {
        setup:
            assert ! cache.nativeCache.isEmpty()
        when:
            cache.clear()
        then:
            cache.nativeCache.isEmpty()
    }

    def 'should remove least recently used entry when full'() {
        setup:
            def cache = createCache(4)
            def keys = ['k1', 'k2', 'k3', 'k4']

        when: 'put 4 entries'
            keys.each { cache.put(it, 'value') }
        then: 'access entries in order and ensure that are here'
            keys.each { assert cache.get(it) }

        when: 'put fifth entry'
            cache.put('k5', 'new-value')
        then: 'that the least recently used k1 disappear'
            ! cache.get('k1')
        and: 'all others are still here'
            cache.get('k5')
            (keys - 'k1').each { assert cache.get(it) }
    }


    def seed(cache, entries=5) {
        entries.times { idx ->
            cache.put("key${idx}", "value${idx}")
        }
    }
}
