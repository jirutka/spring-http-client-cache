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

import net.jcip.annotations.Immutable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.Assert;

import java.io.*;

/**
 * HttpResponseReader implementation that reads a response until the specified
 * size limit.
 */
@Immutable
public class SizeLimitedHttpResponseReader implements HttpResponseReader {

    public static final int DEFAULT_BUFFER_SIZE = 2048;
    private static final double INITIAL_CAPACITY_FACTOR = 0.3;

    private final int maxBodySize;
    private final int bufferSize;

    /**
     * Construct with a {@link #DEFAULT_BUFFER_SIZE default buffer size}.
     *
     * @param maxBodySize The maximal size to read in bytes. It must be
     *                    greater then zero and should be multiple of the
     *                    {@link #DEFAULT_BUFFER_SIZE}.
     */
    public SizeLimitedHttpResponseReader(int maxBodySize) {
        this(maxBodySize, DEFAULT_BUFFER_SIZE);
    }

    /**
     * @param maxBodySize The maximal size to read in bytes. It must be
     *                    greater then zero and should be multiple of the
     *                    {@code bufferSize}.
     * @param bufferSize The buffer size in bytes. It must be greater then zero.
     */
    public SizeLimitedHttpResponseReader(int maxBodySize, int bufferSize) {
        Assert.isTrue(maxBodySize > 0, "bytesLimit must be greater then zero");
        Assert.isTrue(maxBodySize > 0, "bufferSize must be greater then zero");

        this.maxBodySize = maxBodySize;
        this.bufferSize = bufferSize;
    }


    /**
     * Reads the original {@link ClientHttpResponse} to memory, if possible,
     * and returns a serializable copy. If the response's body size exceeds the
     * specified {@code maxBodySize} limit, then it throws
     * {@link ResponseSizeLimitExceededException} with a reconstructed response
     * that combines an already read bytes in memory and the original response.
     *
     * @param response The original response to read.
     * @return An in-memory copy of the original response.
     * @throws ResponseSizeLimitExceededException When the response's body size
     *         exceeds the specified {@code maxBodySize} limit.
     * @throws IOException
     */
    public InMemoryClientHttpResponse readResponse(ClientHttpResponse response)
            throws ResponseSizeLimitExceededException, IOException {

        Assert.notNull(response, "response must not be null");

        InputStream bodyStream = response.getBody();
        ByteArrayOutputStream out = new ByteArrayOutputStream(initialCapacity());

        long bytesTotal = 0;
        byte[] buffer = new byte[bufferSize];
        int bytesRead = -1;

        while ((bytesRead = bodyStream.read(buffer)) != -1) {
            out.write(buffer, 0, bytesRead);
            bytesTotal += bytesRead;

            if (bytesTotal > maxBodySize) {
                throw new ResponseSizeLimitExceededException( createCombinedResponse(response, out, bodyStream) );
            }
        }
        response.close();

        return createInMemoryResponse(response, out);
    }


    private int initialCapacity() {
        return new Double(maxBodySize * INITIAL_CAPACITY_FACTOR).intValue();
    }

    private CombinedClientHttpResponse createCombinedResponse(
            ClientHttpResponse originalResponse, ByteArrayOutputStream consumedBody, InputStream originalBody) {

        InputStream combinedBody = new SequenceInputStream(new ByteArrayInputStream(consumedBody.toByteArray()), originalBody);

        return new CombinedClientHttpResponse(originalResponse, combinedBody);
    }

    private InMemoryClientHttpResponse createInMemoryResponse(
            ClientHttpResponse originalResponse, ByteArrayOutputStream body) throws IOException {

        HttpStatus status = originalResponse.getStatusCode();
        HttpHeaders headers = originalResponse.getHeaders();

        return new InMemoryClientHttpResponse(body.toByteArray(), status, headers);
    }


    //////// Inner class ////////

    public static class ResponseSizeLimitExceededException extends IOException {

        private final ClientHttpResponse response;

        public ResponseSizeLimitExceededException(ClientHttpResponse response) {
            this.response = response;
        }

        /**
         * Returns a reconstructed response that can be read as usual.
         */
        public ClientHttpResponse getResponse() {
            return response;
        }
    }
}
