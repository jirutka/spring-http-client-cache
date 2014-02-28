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

import org.springframework.mock.http.client.MockClientHttpRequest
import spock.lang.Specification

import static org.springframework.http.HttpMethod.GET

class SimpleCacheKeyGeneratorTest extends Specification {

    def generator = new SimpleCacheKeyGenerator()


    def 'generate key for request'() {
        setup:
            def request = new MockClientHttpRequest(method, new URI(uri))
        expect:
            generator.createKey(request) == expected
        where:
            method | uri                                || expected
            GET    | 'http://example.org/path?foo=bar'  || 'GET:http://example.org/path?foo=bar'
    }
}
