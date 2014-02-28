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
package cz.jirutka.spring.http.client.cache;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

/**
 * Policy that determines if a request can be served from cache or a response
 * can be cached.
 */
public interface CachingPolicy {

    /**
     * Determine if the {@link ClientHttpResponse} gotten from the origin is a
     * cacheable response.
     *
     * @param request The request that generated an origin hit.
     * @param response The response from the origin.
     * @return <tt>true</tt> if response is cacheable.
     */
    boolean isResponseCacheable(HttpRequest request, ClientHttpResponse response);

    /**
     * Determines if the given {@code HttpRequest} is allowed to be served
     * from cache.
     *
     * @param request The request to check.
     * @return <tt>true</tt> if request can be served from cache.
     */
    boolean isServableFromCache(HttpRequest request);
}
