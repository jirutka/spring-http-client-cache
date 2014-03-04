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
package cz.jirutka.spring.http.client.cache.test

import org.springframework.http.HttpMethod
import org.springframework.http.HttpRequest
import org.springframework.http.HttpStatus
import org.springframework.http.client.ClientHttpResponse
import org.springframework.mock.http.client.MockClientHttpRequest
import org.springframework.mock.http.client.MockClientHttpResponse

import java.text.SimpleDateFormat

import static org.springframework.http.HttpMethod.GET

class HttpHeadersHelper {

    /**
     * Date format pattern used to parse HTTP date headers in RFC 1123 format.
     */
    public static final DATE_FORMAT_RFC1123 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz");

    public static final EMPTY_BODY = new byte[0]
    public static final SOME_BODY = 'allons-y!'.bytes

    HttpRequest request = buildRequest()
    ClientHttpResponse response = buildResponse()

    Date now = new Date()


    void setResponseHeaders(kwargs = [:]) {
        response = buildResponse(kwargs)
    }

    void setRequestHeaders(kwargs = [:]) {
        request = buildRequest(kwargs)
    }

    HttpRequest buildRequest(kwargs = [:]) {
        kwargs = [method: GET, uri: 'http://example.org'] << kwargs  // default values

        def method = kwargs['method'] as HttpMethod
        def uri = new URI(kwargs['uri'] as String)

        def req = new MockClientHttpRequest(method, uri)
        addHeaders(req, kwargs - kwargs.subMap(['method', 'uri']))

        return req
    }

    ClientHttpResponse buildResponse(kwargs = [:]) {
        kwargs = [body: SOME_BODY, status: 200, Date: new Date(), 'Content-Length': 9] << kwargs  // default values

        def body = (kwargs['body'] as String).bytes
        def status = HttpStatus.valueOf(kwargs['status'] as int)

        def resp = new MockClientHttpResponse(body, status)
        addHeaders(resp, kwargs - kwargs.subMap(['body', 'status']))

        return resp
    }

    String formatDate(Date date) {
        DATE_FORMAT_RFC1123.format(date)
    }


    private addHeaders(httpEntry, headersMap) {
        headersMap.each { key, val ->
            val = val instanceof Date ? formatDate(val) : val
            if (val != null) httpEntry.headers.add(key, val.toString())
        }
    }
}
