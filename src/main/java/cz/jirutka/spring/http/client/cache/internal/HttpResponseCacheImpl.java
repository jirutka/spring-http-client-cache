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

import cz.jirutka.spring.http.client.cache.CacheKeyGenerator;
import cz.jirutka.spring.http.client.cache.DefaultResponseExpirationResolver;
import cz.jirutka.spring.http.client.cache.ResponseExpirationResolver;
import cz.jirutka.spring.http.client.cache.SimpleCacheKeyGenerator;
import cz.jirutka.spring.http.client.cache.internal.SizeLimitedResponseReader.ResponseSizeLimitExceededException;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

import java.io.IOException;
import java.util.Date;

@Slf4j
public class HttpResponseCacheImpl implements HttpResponseCache {

    private final Cache cache;

    private final CacheKeyGenerator keyGenerator;

    @Getter @Setter
    private ResponseExpirationResolver expirationResolver;

    @Getter @Setter
    private SizeLimitedResponseReader responseReader;


    public HttpResponseCacheImpl(Cache cache, boolean sharedCache, int maxResponseSize) {
        this(cache, sharedCache, maxResponseSize, new SimpleCacheKeyGenerator());
    }

    public HttpResponseCacheImpl(Cache cache, boolean sharedCache, int maxResponseSize, CacheKeyGenerator keyGenerator) {
        this.cache = cache;
        this.keyGenerator = keyGenerator;
        this.expirationResolver = new DefaultResponseExpirationResolver(sharedCache);
        this.responseReader = new SizeLimitedResponseReader(maxResponseSize);
    }


    public void clear() {
        cache.clear();
    }

    public void evict(HttpRequest request) {
        cache.evict(toKey(request));
    }

    public CacheEntry getCacheEntry(HttpRequest request) {
        ValueWrapper wrapper = cache.get(toKey(request));

        return wrapper != null ? (CacheEntry) wrapper.get() : null;
    }

    public ClientHttpResponse cacheAndReturnResponse(HttpRequest request, ClientHttpResponse response, Date requestSent, Date responseReceived) throws IOException {
        try {
            InMemoryClientHttpResponse storedResponse = responseReader.readResponseUntilLimit(response);

            Date initialDate = expirationResolver.resolveInitialDate(response, requestSent, responseReceived);
            Date expirationDate = expirationResolver.resolveExpirationDate(response, initialDate);

            cache.put(toKey(request), new CacheEntry(storedResponse, initialDate, expirationDate));

            return storedResponse;

        } catch (ResponseSizeLimitExceededException ex) {
            log.info("[{} {}] {}", request.getMethod(), request.getURI(), "actual content length exceeded the limit");
            return ex.getResponse();
        }
    }


    private String toKey(HttpRequest request) {
        Assert.notNull(request, "request must not be null");
        return keyGenerator.createKey(request);
    }
}
