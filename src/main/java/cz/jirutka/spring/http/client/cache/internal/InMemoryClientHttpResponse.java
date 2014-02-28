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
package cz.jirutka.spring.http.client.cache.internal;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.jcip.annotations.NotThreadSafe;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

@Data
@EqualsAndHashCode(doNotUseGetters=true)
@NotThreadSafe //HttpStatus is mutable
public class InMemoryClientHttpResponse implements ClientHttpResponse, Serializable {

    private static final long serialVersionUID = 1L;

    private final byte[] body;
    private final HttpStatus statusCode;
    private final HttpHeaders headers;


    public InMemoryClientHttpResponse(byte[] body, HttpStatus statusCode, HttpHeaders headers) {
        Assert.notNull(statusCode, "statusCode must not be null");

        this.body = body != null ? body : new byte[0];
        this.statusCode = statusCode;
        this.headers = headers != null ? headers : new HttpHeaders();
    }


    public InputStream getBody() {
        return new ByteArrayInputStream(body);
    }

    public byte[] getBodyAsByteArray() {
        return body;
    }

    public int getRawStatusCode() {
        return statusCode.value();
    }

    public String getStatusText() {
        return statusCode.getReasonPhrase();
    }

    public void close() {
        // do nothing
    }

    public InMemoryClientHttpResponse deepCopy() {
        HttpHeaders headersCopy = new HttpHeaders();
        for (Entry<String, List<String>> entry : headers.entrySet()) {
            headersCopy.put(entry.getKey(), new LinkedList<>(entry.getValue()));
        }
        return new InMemoryClientHttpResponse(body.clone(), statusCode, headersCopy);
    }
}
