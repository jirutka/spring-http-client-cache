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

import lombok.Delegate;
import net.jcip.annotations.NotThreadSafe;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.InputStream;

/**
 * {@link ClientHttpResponse} implementation that combines an existing response
 * with a different body. It delegates all methods to the underlying
 * {@code ClientHttpResponse} expect {@link #getBody()}, that returns the
 * specified body instead of the one inside the underlying response.
 */
@NotThreadSafe
public class CombinedClientHttpResponse implements ClientHttpResponse {

    @Delegate(excludes=HttpInputMessage.class)
    private final ClientHttpResponse response;

    private final InputStream body;


    /**
     * @param response The original response to decorate.
     * @param body The body of the message as an input stream.
     */
    public CombinedClientHttpResponse(ClientHttpResponse response, InputStream body) {
        Assert.notNull(response, "response must not be null");

        this.response = response;
        this.body = body;
    }


    public InputStream getBody() throws IOException {
        return body;
    }

    public HttpHeaders getHeaders() {
        return response.getHeaders();
    }
}
