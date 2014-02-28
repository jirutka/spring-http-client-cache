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
import org.springframework.http.HttpStatus
import spock.lang.Specification

import static org.springframework.util.FileCopyUtils.copyToByteArray

class InMemoryClientHttpResponseTest extends Specification {

    def body = 'allons-y!'.bytes
    def status = HttpStatus.OK
    def headers = new HttpHeaders().with {
        cacheControl = 'max-age=600'
        add 'Pragma', 'foo'
        add 'Pragma', 'bar'
        return it
    }

    def 'construct'() {
        when:
            def actual = new InMemoryClientHttpResponse(body, status, headers)
        then:
            actual.statusCode            == status
            actual.rawStatusCode         == status.value()
            actual.statusText            == status.reasonPhrase
            actual.bodyAsByteArray       == body
            copyToByteArray(actual.body) == body
    }

    def 'deep copy'() {
        given:
            def original = new InMemoryClientHttpResponse(body, status, headers)

        when:
            def copy = original.deepCopy()
        then: 'copy and original should be equal'
            copy == original

        when:
            copy.headers.add('Pragma', 'baz')
        then: 'original is not modified'
            copy.headers != original.headers
    }
}
