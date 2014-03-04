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
package cz.jirutka.spring.http.client.cache.internal

import cz.jirutka.spring.http.client.cache.CacheKeyGenerator
import cz.jirutka.spring.http.client.cache.ResponseExpirationResolver
import cz.jirutka.spring.http.client.cache.internal.SizeLimitedHttpResponseReader.ResponseSizeLimitExceededException
import cz.jirutka.spring.http.client.cache.test.AbbreviatedTimeCategory
import cz.jirutka.spring.http.client.cache.test.HttpHeadersHelper
import org.springframework.cache.Cache
import org.springframework.cache.support.SimpleValueWrapper
import org.springframework.http.HttpHeaders
import spock.lang.Specification
import spock.util.mop.Use

import static org.springframework.http.HttpStatus.OK

@Mixin(HttpHeadersHelper)
@Use(AbbreviatedTimeCategory)
class HttpResponseCacheImplTest extends Specification {

    def cache = Mock(Cache)
    def keyGenerator = Mock(CacheKeyGenerator)
    def expirationResolver = Mock(ResponseExpirationResolver)
    def responseReader = Mock(HttpResponseReader)

    def responseCache = new HttpResponseCacheImpl(cache, true, 1024, keyGenerator)
    def cacheEntry = new CacheEntry(new InMemoryClientHttpResponse(SOME_BODY, OK, new HttpHeaders()), now, now)


    void setup() {
        responseCache.expirationResolver = expirationResolver
        responseCache.responseReader = responseReader
    }

    def 'clear: should clear the cache'() {
        when:
            responseCache.clear()
        then:
            1 * cache.clear()
    }

    def 'evict: should evict cache entry for request'() {
        setup:
            keyGenerator.createKey(request) >> 'super-key'
        when:
            responseCache.evict(request)
        then:
            1 * cache.evict('super-key')
    }

    def 'getCacheEntry: should get cached entry from cache when exists'() {
        setup:
            keyGenerator.createKey(request) >> 'super-key'
        when:
            def returned = responseCache.getCacheEntry(request)
        then:
            cache.get('super-key') >> new SimpleValueWrapper(cacheEntry)
        and:
            returned == cacheEntry
    }

    def 'getCacheEntry: should return null when unknown key'() {
        setup:
            keyGenerator.createKey(request) >> 'wrong-key'
        when:
            def returned = responseCache.getCacheEntry(request)
        then:
            1 * cache.get('wrong-key') >> null
        and:
            returned == null
    }

    def 'cacheAndReturnResponse: should cache response'() {
        setup:
            def reqSent = now - 2.sec
            def respReceived = now
            def initDate = now - 1.min
            def expDate = now + 5.min
            def fetchedResponse = cacheEntry.response
        and:
            keyGenerator.createKey(request) >> 'cool-key'
        when:
            def returned = responseCache.cacheAndReturnResponse(request, response, reqSent, respReceived)
        then:
            1 * responseReader.readResponse(response) >> fetchedResponse
        and:
            1 * expirationResolver.resolveInitialDate(fetchedResponse, reqSent, respReceived) >> initDate
            1 * expirationResolver.resolveExpirationDate(fetchedResponse, initDate) >> expDate
        and:
            cache.put('cool-key', new CacheEntry(fetchedResponse, initDate, expDate))
        and:
            returned == fetchedResponse
    }

    def 'cacheAndReturnResponse: should NOT cache response when size limit exceeds'() {
        setup:
            def rejectedResponse = new CombinedClientHttpResponse(response, new ByteArrayInputStream('too-big'.bytes))
        and:
            1 * responseReader.readResponse(_) >> { throw new ResponseSizeLimitExceededException(rejectedResponse) }
            0 * cache._
        when:
            def returned = responseCache.cacheAndReturnResponse(request, response, now, now)
        then:
            returned == rejectedResponse
    }
}
