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

import lombok.EqualsAndHashCode;
import net.jcip.annotations.Immutable;
import org.springframework.http.client.ClientHttpResponse;

import java.io.Serializable;
import java.util.Date;

@Immutable
@EqualsAndHashCode
public class CacheEntry implements Serializable {

    private static final long serialVersionUID = 1L;

    private final InMemoryClientHttpResponse response;
    private final Date responseCreated;
    private final Date responseExpiration;

    /**
     * @param response The response to cache.
     * @param responseCreated When the response was originally created.
     * @param responseExpiration When the response will expire.
     */
    public CacheEntry(InMemoryClientHttpResponse response, Date responseCreated, Date responseExpiration) {
        this.response = response;
        this.responseCreated = responseCreated;
        this.responseExpiration = responseExpiration;
    }


    public ClientHttpResponse getResponse() {
        return response.deepCopy();
    }

    public Date getResponseCreated() {
        return new Date(responseCreated.getTime());
    }

    public Date getResponseExpiration() {
        return new Date(responseExpiration.getTime());
    }
}
