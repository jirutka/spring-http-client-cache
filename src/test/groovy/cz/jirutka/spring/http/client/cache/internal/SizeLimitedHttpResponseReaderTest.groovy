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

import cz.jirutka.spring.http.client.cache.internal.SizeLimitedHttpResponseReader.ResponseSizeLimitExceededException
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import spock.lang.Specification

import static org.springframework.util.FileCopyUtils.copyToByteArray

class SizeLimitedHttpResponseReaderTest extends Specification {

    static bufferSize = 128

    def response = Mock(ClientHttpResponse)

    void setup() {
        response.statusCode >> HttpStatus.OK
    }


    def 'read response when body does not reach limit'() {
        setup:
            def expectedBody = generateBodyOfLength(bodyLength)
            def bodyStream = new ByteArrayInputStream(expectedBody)
            def reader = new SizeLimitedHttpResponseReader(limit, bufferSize)
        and:
            response.body >> bodyStream

        when:
            def readResponse = reader.readResponse(response)
        then: 'body stream was completely read'
            readResponse instanceof InMemoryClientHttpResponse
        and:
            bodyStream.available() == 0
        and: 'response was closed'
            1 * response.close()

        when: 'read the returned response copy'
            def actualBody = copyToByteArray(readResponse.body)
        then:
            actualBody == expectedBody

        where:
            bodyLength | limit
            100        | 2048
            1024       | 2048
    }

    def 'read response when body exceeds size limit'() {
        setup:
            def expectedBody = generateBodyOfLength(bodyLength)
            def bodyStream = new ByteArrayInputStream(expectedBody)
            def reader = new SizeLimitedHttpResponseReader(limit, bufferSize)
        and:
            response.body >> bodyStream

        when:
            reader.readResponse(response)
        then:
            def ex = thrown(ResponseSizeLimitExceededException)
            ex.response instanceof CombinedClientHttpResponse
        and: 'body stream was read until the limit rounded by buffer size'
            bodyStream.available() == overlap
        and: 'original response was not closed'
            0 * response.close()

        when: 'read the returned response copy'
            def actualBody = copyToByteArray(ex.response.body)
        then:
            actualBody == expectedBody
        and: 'the original body stream is now completely read'
            bodyStream.available() == 0

        where:
            bodyLength | limit | overlap
            280        | 255   | 24
            280        | 130   | 24
            170        | 160   | 0
    }


    def generateBodyOfLength(int length) {
        ('x' * length).bytes
    }
}
