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
import lombok.NoArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.util.Assert;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a HTTP Cache-Control response header and parses it from string.
 *
 * <p>Note: This class ignores <tt>1#field-name</tt> parameter for
 * <tt>private</tt> and <tt>no-cache</tt> directive and cache extensions.</p>
 *
 * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9">HTTP/1.1 section 14.9</a>
 */
@Data
@NoArgsConstructor
public class CacheControl {

    // copied from org.apache.abdera.protocol.util.CacheControlUtil
    private static final Pattern PATTERN
            = Pattern.compile("\\s*([\\w\\-]+)\\s*(=)?\\s*(\\-?\\d+|\\\"([^\"\\\\]*(\\\\.[^\"\\\\]*)*)+\\\")?\\s*");

    /**
     * Corresponds to the <tt>max-age</tt> cache control directive.
     * The default value is <tt>-1</tt>, i.e. not specified.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.3">HTTP/1.1 section 14.9.3</a>
     */
    private int maxAge = -1;

    /**
     * Corresponds to the <tt>s-maxage</tt> cache control directive.
     * The default value is <tt>-1</tt>, i.e. not specified.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.3">HTTP/1.1 section 14.9.3</a>
     */
    private int sMaxAge = -1;

    /**
     * Whether the <tt>must-revalidate</tt> directive is specified.
     * The default value is <tt>false</tt>.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.4">HTTP/1.1 section 14.9.4</a>
     */
    private boolean isMustRevalidate = false;

    /**
     * Whether the <tt>no-cache</tt> directive is specified.
     * The default value is <tt>false</tt>.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.1">HTTP/1.1 section 14.9.1</a>
     */
    private boolean isNoCache = false;

    /**
     * Whether the <tt>no-store</tt> directive is specified.
     * The default value is <tt>false</tt>.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.2">HTTP/1.1 section 14.9.2</a>
     */
    private boolean isNoStore = false;

    /**
     * Whether the <tt>no-transform</tt> directive is specified.
     * The default value is <tt>false</tt>.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.5">HTTP/1.1 section 14.9.5</a>
     */
    private boolean isNoTransform = false;

    /**
     * Whether the <tt>private</tt> directive is specified.
     * The default value is <tt>false</tt>.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.1">HTTP/1.1 section 14.9.1</a>
     */
    private boolean isPrivate = false;

    /**
     * Whether the <tt>public</tt> directive is specified.
     * The default value is <tt>false</tt>.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.1">HTTP/1.1 section 14.9.1</a>
     */
    private boolean isPublic = false;

    /**
     * Whether the <tt>proxy-revalidate</tt> directive is specified.
     * The default value is <tt>false</tt>.
     *
     * @see <a href="http://www.w3.org/Protocols/rfc2616/rfc2616-sec14.html#sec14.9.4">HTTP/1.1 section 14.9.4</a>
     */
    private boolean isProxyRevalidate = false;


    /**
     * Creates a new instance of CacheControl by parsing the supplied string.
     *
     * @param value A value the Cache-Control header.
     */
    public static CacheControl valueOf(String value) {
        CacheControl cc = new CacheControl();

        if (value != null) {
            Matcher matcher = PATTERN.matcher(value);
            while (matcher.find()) {
                switch (matcher.group(1).toLowerCase()) {
                    case "max-age":
                        cc.setMaxAge(Integer.parseInt(matcher.group(3))); break;
                    case "s-maxage":
                        cc.setSMaxAge(Integer.parseInt(matcher.group(3))); break;
                    case "must-revalidate":
                        cc.setMustRevalidate(true); break;
                    case "no-cache":
                        cc.setNoCache(true); break;
                    case "no-store":
                        cc.setNoStore(true); break;
                    case "no-transform":
                        cc.setNoTransform(true); break;
                    case "private":
                        cc.setPrivate(true); break;
                    case "public":
                        cc.setPublic(true); break;
                    case "proxy-revalidate":
                        cc.setProxyRevalidate(true); break;
                    default: //ignore
                }
            }
        }
        return cc;
    }

    public static CacheControl parseCacheControl(HttpHeaders headers) {
        Assert.notNull(headers, "headers must not be null");

        return valueOf(headers.getCacheControl());
    }

    /**
     * Returns <tt>max-age</tt>, or <tt>s-maxage</tt> according to whether
     * considering a shared cache, or a private cache. If shared cache and the
     * <tt>s-maxage</tt> is negative (i.e. not set), then returns
     * <tt>max-age</tt> instead.
     *
     * @param sharedCache <tt>true</tt> for a shared cache,
     *                    or <tt>false</tt> for a private cache
     * @return A {@link #maxAge}, or {@link #sMaxAge} according to the given
     *         sharedCache argument.
     */
    public int getMaxAge(boolean sharedCache) {
        if (sharedCache) {
            return sMaxAge >= 0 ? sMaxAge : maxAge;
        } else {
            return maxAge;
        }
    }
}
