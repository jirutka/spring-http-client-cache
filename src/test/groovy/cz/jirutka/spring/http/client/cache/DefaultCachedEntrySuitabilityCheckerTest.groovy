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

import cz.jirutka.spring.http.client.cache.internal.CacheEntry
import cz.jirutka.spring.http.client.cache.internal.InMemoryClientHttpResponse
import cz.jirutka.spring.http.client.cache.test.AbbreviatedTimeCategory
import cz.jirutka.spring.http.client.cache.test.HttpHeadersHelper
import org.springframework.http.HttpHeaders
import spock.lang.Shared
import spock.lang.Specification
import spock.util.mop.Use

import static org.springframework.http.HttpStatus.OK

@Use(AbbreviatedTimeCategory)
@Mixin(HttpHeadersHelper)
class DefaultCachedEntrySuitabilityCheckerTest extends Specification {

    @Shared checker = new DefaultCachedEntrySuitabilityChecker()


    def 'suitable if cached is fresh'() {
        given:
            def entry = createCacheEntry(now -1.hour, now +1.hour)
        expect:
            assertSuitable entry
    }

    def 'not suitable if cached is expired'() {
        given:
            def entry = createCacheEntry(now -2.hour, now -1.hour)
        expect:
            assertNotSuitable(entry)
    }

    def 'suitable if cached is fresh and age is under request max-age'() {
        given:
            requestHeaders  = ['Cache-Control': 'max-age=90']
            def entry = createCacheEntry(now -60.sec, now +1.hour)
        expect:
           assertSuitable(entry)
    }

    def 'not suitable if age exceeds request max-age'() {
        given:
            requestHeaders  = ['Cache-Control': 'max-age=10']
            def entry = createCacheEntry(now -30.min, now +1.hour)
        expect:
            assertNotSuitable(entry)
    }


    def createCacheEntry(Date initialDate, Date expirationDate) {
        new CacheEntry(new InMemoryClientHttpResponse('foo'.bytes, OK, new HttpHeaders()), initialDate, expirationDate)
    }

    void assertSuitable(cachedResponse) {
        assert checker.canCachedEntryBeUsed(request, cachedResponse, now)
    }

    void assertNotSuitable(cachedResponse) {
        assert ! checker.canCachedEntryBeUsed(request, cachedResponse, now)
    }
}
