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

import org.springframework.http.HttpHeaders
import spock.lang.Specification
import spock.lang.Unroll

class CacheControlTest extends Specification {

    @Unroll
    def "parse cache header: #value"() {
        expect:
            CacheControl.valueOf(value) == expected
        and:
            CacheControl.parseCacheControl(headers) == expected
        where:
            value                   | expected
            ''                      | new CacheControl()
            'must-revalidate'       | new CacheControl(mustRevalidate: true)
            'No-CaChE'              | new CacheControl(noCache: true)
            ' no-store '            | new CacheControl(noStore: true)
            'NO-TRANSFORM'          | new CacheControl(noTransform: true)
            'private'               | new CacheControl(isPrivate: true)
            'public'                | new CacheControl(isPublic: true)
            'proxy-revalidate'      | new CacheControl(proxyRevalidate: true)
            'max-age=60'            | new CacheControl(maxAge: 60)
            's-MaxAge = 30 '        | new CacheControl(sMaxAge: 30)
            'public, max-age=60'    | new CacheControl(isPublic: true, maxAge: 60)
            'foo="bar 12", no-cache'| new CacheControl(noCache: true)

            headers = new HttpHeaders(cacheControl: value)
    }

    def 'should return maxAge according to cache type'() {
        when:
            def cc = new CacheControl(maxAge: maxAge, sMaxAge: sMaxAge)
        then:
            cc.getMaxAge(shared) == expected
        where:
            shared || maxAge | sMaxAge || expected
            false  || 60     | 30      || 60
            true   || -1     | 30      || 30
            true   || 60     | 30      || 30
            true   || 60     | 0       || 0
            true   || 60     | -1      || 60
    }
}
