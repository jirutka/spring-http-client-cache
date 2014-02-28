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

import cz.jirutka.spring.http.client.cache.internal.CacheControl;
import net.jcip.annotations.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static cz.jirutka.spring.http.client.cache.internal.CacheControl.parseCacheControl;
import static java.util.Arrays.asList;

/**
 * Policy that determines if a request can be served from cache or a response
 * can be cached.
 *
 * <p>This implementation currently supports HTTP/1.1 <tt>Cache-Control</tt>
 * header only, no <tt>Expires</tt> etc.</p>
 */
@Immutable
public class DefaultCachingPolicy implements CachingPolicy {

    private static final Logger log = LoggerFactory.getLogger(DefaultCachingPolicy.class);

    private static final Set<HttpMethod> CACHEABLE_METHODS = EnumSet.of(HttpMethod.GET);

    /**
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec13.html#sec13.4">HTTP/1.1 section 13.4</a>
     */
    private static final Set<Integer> CACHEABLE_STATUSES = new HashSet<>(asList(200, 203, 300, 301, 410));

    private static final Set<Integer> UNCACHEABLE_STATUSES = new HashSet<>(asList(206, 303));


    private final long maxBodySizeBytes;

    private final boolean sharedCache;


    /**
     * Creates a new instance of {@code DefaultCachingPolicy} without the
     * size limit.
     *
     * @param sharedCache Whether to behave as a shared cache (true) or a
     *                    non-shared/private cache (false).
     * @see #DefaultCachingPolicy(boolean, long)
     */
    public DefaultCachingPolicy(boolean sharedCache) {
        this(sharedCache, Long.MAX_VALUE);
    }

    /**
     * Creates a new instance of {@code DefaultCachingPolicy} with defined
     * size limit of responses that should be stored in the cache.
     *
     * <p>A private cache will not, for example, cache responses to requests
     * with Authorization headers or responses marked with <tt>Cache-Control:
     * private</tt>. If, however, the cache is only going to be used by one
     * logical "user" (behaving similarly to a browser cache), then you will
     * want to turn off the shared cache setting.</p>
     *
     * @param maxBodySizeBytes The maximum content length.
     * @param sharedCache Whether to behave as a shared cache (true) or a
     *                    non-shared/private cache (false).
     */
    public DefaultCachingPolicy(boolean sharedCache, long maxBodySizeBytes) {
        this.sharedCache = sharedCache;
        this.maxBodySizeBytes = maxBodySizeBytes > 0 ? maxBodySizeBytes : Long.MAX_VALUE;
    }


    public boolean isResponseCacheable(HttpRequest request, ClientHttpResponse response) {
        HttpHeaders reqHeaders = request.getHeaders();
        HttpHeaders respHeaders = response.getHeaders();

        if (!isCacheableMethod(request.getMethod())) {
            log.trace("Not cacheable: method {}", request.getMethod());
            return false;
        }

        if (parseCacheControl(reqHeaders).isNoStore()) {
            log.trace("Not cacheable: request has Cache-Control: no-store");
            return false;
        }

        if (sharedCache) {
            if (reqHeaders.getFirst("Authorization") != null) {
                CacheControl cc = parseCacheControl(respHeaders);
                if (!cc.isPublic() && cc.getSMaxAge() <= 0) {
                    log.trace("Not cacheable: this cache is shared and request contains " +
                              "Authorization header, but no Cache-Control: public");
                    return false;
                }
            }
        }
        return isResponseCacheable(response);
    }

    public boolean isServableFromCache(HttpRequest request) {

        if (!isCacheableMethod(request.getMethod())) {
            log.trace("Request with method {} is not serveable from cache", request.getMethod());
            return false;
        }
        CacheControl cc = parseCacheControl(request.getHeaders());

        if (cc.isNoStore()) {
            log.trace("Request with no-store is not serveable from cache");
            return false;
        }
        if (cc.isNoCache()) {
            log.trace("Request with no-cache is not serveable from cache");
            return false;
        }

        return true;
    }


    protected boolean isResponseCacheable(ClientHttpResponse response) {

        boolean cacheable = false;
        HttpHeaders headers = response.getHeaders();

        try {
            int status = response.getRawStatusCode();
            if (isImplicitlyCacheableStatus(status)) {
                cacheable = true;  //MAY be cached

            } else if (isUncacheableStatus(status)) {
                log.trace("Response with status code {} is not cacheable", status);
                return false;
            }
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        if (isExplicitlyNonCacheable(response)) {
            log.trace("Response with Cache-Control: '{}' is not cacheable", headers.getCacheControl());
            return false;
        }

        if (headers.getContentLength() > maxBodySizeBytes) {
            log.debug("Response with Content-Lenght {} > {} is not cacheable",
                    headers.getContentLength(), maxBodySizeBytes);
            return false;
        }

        try {
            if (response.getHeaders().getDate() < 0) {
                log.debug("Response without a valid Date header is not cacheable");
                return false;
            }
        } catch (IllegalArgumentException ex) {
            return false;
        }

        // dunno how to properly handle Vary
        if (headers.containsKey("Vary")) {
            log.trace("Response with Vary header is not cacheable");
            return false;
        }

        return (cacheable || isExplicitlyCacheable(response));
    }

    /**
     * Whether the given status code can be cached implicitly, i.e. even when
     * no cache header is specified.
     *
     * @param status HTTP status code
     */
    protected boolean isImplicitlyCacheableStatus(int status) {
        return CACHEABLE_STATUSES.contains(status);
    }

    /**
     * Whether the given status code must not be cached, even when any cache
     * header is specified.
     *
     * @param status HTTP status code
     */
    protected boolean isUncacheableStatus(int status) {
        return UNCACHEABLE_STATUSES.contains(status) || isUnknownStatus(status);
    }

    /**
     * Whether the given status code is considered to unknown and thus must not
     * be cached.
     *
     * <i>The unknown statuses list is based on Apache HTTP Components.</i>
     *
     * @param status HTTP status code
     */
    protected boolean isUnknownStatus(int status) {
        return ! (status >= 100 && status <= 101
               || status >= 200 && status <= 206
               || status >= 300 && status <= 307
               || status >= 400 && status <= 417
               || status >= 500 && status <= 505);
    }

    protected boolean isCacheableMethod(HttpMethod method) {
        return CACHEABLE_METHODS.contains(method);
    }

    /**
     * Whether the given response must not be cached.
     */
    protected boolean isExplicitlyNonCacheable(ClientHttpResponse response) {
        CacheControl cc = parseCacheControl(response.getHeaders());

        return cc.isNoStore()
                || cc.isNoCache()
                || (sharedCache && cc.isPrivate())
                || cc.getMaxAge(sharedCache) == 0;
    }

    /**
     * Whether the given response should be cached.
     */
    protected boolean isExplicitlyCacheable(ClientHttpResponse response) {
        CacheControl cc = parseCacheControl(response.getHeaders());

        return cc.isPublic()
                || cc.isMustRevalidate()
                || cc.isProxyRevalidate()
                || cc.getMaxAge(sharedCache) > 0;
    }
}
