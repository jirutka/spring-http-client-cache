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

import cz.jirutka.spring.http.client.cache.internal.CacheEntry;
import cz.jirutka.spring.http.client.cache.internal.HttpResponseCache;
import cz.jirutka.spring.http.client.cache.internal.HttpResponseCacheImpl;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.Date;

@Slf4j
@Getter @Setter
public class CachingHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    /**
     * The cache implementation used for caching.
     */
    private final HttpResponseCache cache;

    private CachingPolicy cachingPolicy;

    private CachedEntrySuitabilityChecker cachedChecker;


    public CachingHttpRequestInterceptor(Cache cache, boolean sharedCache, int maxResponseSize) {
        this.cache = new HttpResponseCacheImpl(cache, sharedCache, maxResponseSize);
        this.cachingPolicy = new DefaultCachingPolicy(sharedCache, maxResponseSize);
        this.cachedChecker = new DefaultCachedEntrySuitabilityChecker();
    }

    public CachingHttpRequestInterceptor(HttpResponseCache cache, CachingPolicy cachingPolicy, CachedEntrySuitabilityChecker cachedChecker) {
        this.cache = cache;
        this.cachingPolicy = cachingPolicy;
        this.cachedChecker = cachedChecker;
    }


    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {

        if (!cachingPolicy.isServableFromCache(request)) {
            log("not servable from cache", request);
            return execute(request, body, execution);
        }

        CacheEntry entry = cache.getCacheEntry(request);
        if (entry == null || !cachedChecker.canCachedEntryBeUsed(request, entry, currentDate())) {
            log("cache miss", request);
            return execute(request, body, execution);

        } else {
            log("cache hit", request);
            return createResponse(entry);
        }
    }


    protected ClientHttpResponse execute(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        final Date requestDate = currentDate();

        ClientHttpResponse response = execution.execute(request, body);

        if (cachingPolicy.isResponseCacheable(request, response)) {
            log("caching response", request);
            return cache.cacheAndReturnResponse(request, response, requestDate, currentDate());

        } else {
            log("response is not cacheable", request);
            return response;
        }
    }

    protected ClientHttpResponse createResponse(CacheEntry entry) {
        ClientHttpResponse response = entry.getResponse();

        Long age = (currentDate().getTime() - entry.getResponseCreated().getTime()) / 1000L;
        response.getHeaders().set("Age", age.toString());

        return response;
    }


    private void log(String message, HttpRequest request) {
        log.debug("[{} {}] {}", request.getMethod(), request.getURI(), message);
    }

    private Date currentDate() {
        return new Date();
    }
}
