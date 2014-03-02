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

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Date;

public interface HttpResponseCache {

    void clear();

    void evict(HttpRequest request);

    /**
     * Returns a cached response for the given request.
     *
     * @param request The request whose associated response is to be returned.
     * @return A cached response for the given request, or {@code null} if
     *         this cache contains no entry for the request.
     */
    CacheEntry getCacheEntry(HttpRequest request);

    /**
     * Store {@link org.springframework.http.client.ClientHttpResponse}, if possible, and return it.
     *
     * @param request The request with which the given response is to be associated.
     * @param response The response object to be cached.
     * @param requestSent When the request was send.
     * @param responseReceived When the response was received.
     */
    ClientHttpResponse cacheAndReturnResponse(
            HttpRequest request, ClientHttpResponse response, Date requestSent, Date responseReceived) throws IOException;
}
