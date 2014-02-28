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

import cz.jirutka.spring.http.client.cache.test.AbbreviatedTimeCategory
import cz.jirutka.spring.http.client.cache.test.HttpHeadersHelper
import org.springframework.http.HttpHeaders
import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Specification
import spock.util.mop.Use

import static cz.jirutka.spring.http.client.cache.DefaultResponseExpirationResolver.MAX_AGE

@Mixin(HttpHeadersHelper)
@Use(AbbreviatedTimeCategory)
class DefaultResponseExpirationResolverTest extends Specification {

    @Shared resolver = new DefaultResponseExpirationResolver()


    def 'parse age header'() {
        given:
            def headers = new HttpHeaders()
            values.each { headers.add('Age', it.toString()) }
        expect:
            resolver.parseAgeHeader(headers) == expected
        where:
            values    | expected
            [60]      | 60
            [0]       | 0
            []        | 0
            [-10]     | MAX_AGE
            [5,20,10] | 20
            ['inv']   | MAX_AGE
            ['xx', 9] | MAX_AGE
    }

    /**
     * apparentAge          = max(0, responseDate - headerDate) / 1000;
     * correctedReceivedAge = max(apparentAge, ageValue);
     * responseDelay        = responseDate - requestDate / 1000;
     * correctedInitialAge  = correctedReceivedAge + responseDelay;
     */
    def 'calculate corrected initial age'() {
        setup:
            responseHeaders = [Date: headerDate, Age: ageValue]
        and:
            resolver.correctedInitialAge(response, requestDate, responseDate) == expected
        where:
            ageValue | headerDate   | responseDate | requestDate || expected
            // use header Age
            60       | now -10.sec  | now          | now         || 60
            'malf'   | now -10.sec  | now          | now         || MAX_AGE
            60       | now          | now          | now -10.sec || 70
            // calculate from header Date
            10       | now -60.sec  | now          | now         || 60
            0        | now          | now -60.min  | now -60.min || 0
            0        | now -60.sec  | now          | now -10.sec || 70
    }

    @Ignore('not implemented yet')
    def 'resolve corrected initial date'() {
        expect:
            false //TODO
    }

    @Ignore('not implemented yet')
    def 'resolve expiration date'() {
        expect:
            false //TODO
    }
}
