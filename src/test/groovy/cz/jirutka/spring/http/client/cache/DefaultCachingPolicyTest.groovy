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

import cz.jirutka.spring.http.client.cache.test.HttpHeadersHelper
import org.springframework.http.HttpMethod
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import static org.springframework.http.HttpMethod.GET

@Mixin(HttpHeadersHelper)
class DefaultCachingPolicyTest extends Specification {

    @Shared policyPrivate = new DefaultCachingPolicy(false, 1024)
    @Shared policyShared = new DefaultCachingPolicy(true, 1024)

    @Shared explicitlyCacheable = ['Cache-Control': 'public,max-age=360']

    def policy = policyPrivate


    //////// isResponseCacheable() ////////

    def 'request with Cache-Control: no-store should not be cacheable ever'() {
        given:
            policy          = _policy
            requestHeaders  = ['Cache-Control': 'no-store']
            responseHeaders = explicitlyCacheable
        expect:
            assertNotCacheable()
        where:
            _policy << [policyShared, policyPrivate]
    }

    def 'request with Authorization should be cacheable by private cache'() {
        given:
            requestHeaders  = [Authorization: 'Basic foobar=']
            responseHeaders = ['Cache-Control': cacheControl]
        expect:
            assertCacheable()
        where:
            cacheControl << ['', 'public', 'private', 's-maxage=0']
    }

    @Unroll
    def 'request with Authorization #statement cacheable by shared cache when "Cache-Control: #cacheControl"'() {
        given:
            policy          = policyShared
            requestHeaders  = [Authorization: 'Basic foobar=']
            responseHeaders = ['Cache-Control': cacheControl]
        expect:
            isCacheable() == expected
        where:
            cacheControl << ['public', 's-maxage=60'] +
                            ['', 's-maxage=0', 'public,s-maxage=0', 'max-age=60', 'private', 'private,s-maxage=60']
            expected     << [true ] * 2 +
                            [false] * 6
            statement = expected ? 'should be' : 'should NOT be'
    }

    @Unroll
    def 'method #method should NOT be cacheable ever'() {
        given:
            requestHeaders  = [method: method]
            responseHeaders = explicitlyCacheable
        expect:
            assertNotCacheable()
        where:
           method << HttpMethod.values() - GET
    }

    @Unroll
    def 'status #status should be cacheable implicitly'() {
        given:
            responseHeaders = [status: status]
        expect:
            assertCacheable()
        where:
            status << [200, 203, 300, 301, 410]
    }

    @Unroll
    def 'status #status should NOT be cacheable ever'() {
        given:
            responseHeaders = explicitlyCacheable << [status: status]
        expect:
            assertNotCacheable()
        where:
            status << [206, 303]
    }

    @Unroll
    def 'status #status should be explicitly cacheable but not implicitly'() {
        when:
            responseHeaders = [status: status]
        then:
            assertNotCacheable()
        when:
            responseHeaders = explicitlyCacheable << [status: status]
        then:
            assertCacheable()
        where:
            // based on Apache HTTP Components
            status << [100, 101, 201, 202, 204, 205, 302, 304, 305, 307] + (400..409) + (411..417) + (501..505)
    }

    def 'Content-Length greater then limit should NOT be cacheable ever'() {
        setup:
            def limit = 1024
        and:
            policy          = new DefaultCachingPolicy(false, limit)
            responseHeaders = explicitlyCacheable << ['Content-Length': contentLength]
        expect:
            isCacheable() == contentLength <= 1024
        where:
            contentLength << [0, 1024, 2048]
    }

    def 'without valid Date header is NOT cacheable ever'() {
        given:
            responseHeaders = [Date: date]
        expect:
            assertNotCacheable()
        where:
            date << [null, 'invalid']
    }

    def 'header Vary should NOT be cacheable'() {
        given:
            responseHeaders = [Vary: '*']
        expect:
            assertNotCacheable()
    }

    @Unroll
    def 'Cache-Control: "#cacheControl" should NOT be cacheable ever'() {
        setup:
            responseHeaders = explicitlyCacheable << ['Cache-Control': cacheControl]
        expect:
            ! isCacheable(policyShared)
        and:
            ! isCacheable(policyPrivate)
        where:
           cacheControl << ['no-store', 'no-cache', 'max-age=0']
    }


    @Unroll
    def 'Cache-Control: "#cacheControl" should NOT be cacheable in shared cache'() {
        given:
            policy          = policyShared
            responseHeaders = explicitlyCacheable << ['Cache-Control': cacheControl]
        expect:
            assertNotCacheable()
        where:
            cacheControl << ['private', 's-maxage=0', 'max-age=60,s-maxage=0']
    }


    //////// isServableFromCache() ////////

    @Unroll
    def '#method request should NOT be servable from cache'() {
        given:
            requestHeaders = [method: method]
        expect:
            ! policy.isServableFromCache(request)
        where:
            method << HttpMethod.values() - GET
    }

    @Unroll
    def 'request with #cacheControl should NOT be servable from cache'() {
        given:
            requestHeaders = ['Cache-Control': cacheControl]
        expect:
            ! policy.isServableFromCache(request)
        where:
            cacheControl << ['no-store', 'no-cache']
    }


    //////// Helper methods ////////

    boolean isCacheable(_policy = policy) {
        _policy.isResponseCacheable(request, response)
    }

    void assertCacheable() {
        assert policy.isResponseCacheable(request, response)
    }

    void assertNotCacheable() {
        assert ! policy.isResponseCacheable(request, response)
    }
}
