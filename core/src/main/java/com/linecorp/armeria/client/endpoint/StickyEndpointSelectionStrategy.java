/*
 * Copyright 2017 LINE Corporation
 *
 * LINE Corporation licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.linecorp.armeria.client.endpoint;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.function.ToLongFunction;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.Endpoint;
import com.linecorp.armeria.client.endpoint.DefaultEndpointSelector.LoadBalancerFactory;
import com.linecorp.armeria.common.HttpRequest;
import com.linecorp.armeria.common.annotation.Nullable;
import com.linecorp.armeria.common.loadbalancer.LoadBalancer;

/**
 * An {@link EndpointSelector} strategy which implements sticky load-balancing using
 * user passed {@link ToLongFunction} to compute hashes for consistent hashing.
 *
 * <p>This strategy can be useful when all requests that qualify some given criteria must be sent to the same
 * backend server. A common use case is to send all requests for the same logged-in user to the same backend,
 * which could have a local cache keyed by user id.
 *
 * <p>In below example, created strategy will route all {@link HttpRequest} which have the same value for key
 * "cookie" of its header to the same server:
 *
 * <pre>{@code
 * ToLongFunction<ClientRequestContext> hasher = (ClientRequestContext ctx) -> {
 *     return ((HttpRequest) ctx.request()).headers().get(HttpHeaderNames.COOKIE).hashCode();
 * };
 * final StickyEndpointSelectionStrategy strategy = new StickyEndpointSelectionStrategy(hasher);
 * }</pre>
 */
final class StickyEndpointSelectionStrategy
        implements EndpointSelectionStrategy,
                   LoadBalancerFactory<LoadBalancer<Endpoint, ClientRequestContext>> {

    private final ToLongFunction<? super ClientRequestContext> requestContextHasher;

    /**
     * Creates a new {@link StickyEndpointSelectionStrategy}
     * with provided hash function to hash a {@link ClientRequestContext} to a {@code long}.
     *
     * @param requestContextHasher The default {@link ToLongFunction} of {@link ClientRequestContext}
     */
    StickyEndpointSelectionStrategy(ToLongFunction<? super ClientRequestContext> requestContextHasher) {
        this.requestContextHasher = requireNonNull(requestContextHasher, "requestContextHasher");
    }

    /**
     * Creates a new sticky {@link EndpointSelector}.
     */
    @Override
    public EndpointSelector newSelector(EndpointGroup endpointGroup) {
        return new DefaultEndpointSelector<>(endpointGroup, this);
    }

    @Override
    public LoadBalancer<Endpoint, ClientRequestContext> newLoadBalancer(
            @Nullable LoadBalancer<Endpoint, ClientRequestContext> oldLoadBalancer, List<Endpoint> candidates) {
        return LoadBalancer.ofSticky(candidates, requestContextHasher);
    }
}
