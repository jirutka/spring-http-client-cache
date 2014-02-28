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

import org.springframework.http.client.ClientHttpResponse
import spock.lang.Specification
import spock.lang.Unroll

@Unroll
class CombinedClientHttpResponseTest extends Specification {

    def response = Mock(ClientHttpResponse)
    def body = new ByteArrayInputStream('allons-y!'.bytes)
    def combined = new CombinedClientHttpResponse(response, body)


    def 'should delegate #method () to underlying response'() {
        when:
            combined.invokeMethod(method, null)
        then:
            1 * response."$method"()
        where:
            method << ClientHttpResponse.methods*.name - 'getBody'
    }

    def "should return given body"() {
        when:
            def actual = combined.getBody()
        then:
            actual == body
        and:
            0 * response._
    }
}
