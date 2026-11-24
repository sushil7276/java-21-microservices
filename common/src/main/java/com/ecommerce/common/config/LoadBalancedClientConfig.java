package com.ecommerce.common.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.restclient.RestClientCustomizer;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * How one service finds another.
 *
 * In the code we write   http://product-service/api/products/1   - there is no host or port.
 * The "@LoadBalanced" builder asks Eureka "where is product-service running?" and picks an instance.
 *
 * We need TWO builders:
 *   1. a normal one (@Primary) - used by Spring itself (e.g. the Eureka client, which must NOT ask Eureka where Eureka is)
 *   2. the @LoadBalanced one   - used by our *Client classes to call other microservices
 *
 * Both have timeouts, so a hanging service can never block us forever.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass({RestClient.class, LoadBalanced.class, RestClientCustomizer.class})
public class LoadBalancedClientConfig {

    @Bean
    @Primary
    @Scope("prototype")
    RestClient.Builder plainRestClientBuilder(ObjectProvider<RestClientCustomizer> customizers) {
        return newBuilder(customizers);
    }

    @Bean
    @LoadBalanced
    @Scope("prototype")
    RestClient.Builder loadBalancedRestClientBuilder(ObjectProvider<RestClientCustomizer> customizers) {
        return newBuilder(customizers);
    }

    private static RestClient.Builder newBuilder(ObjectProvider<RestClientCustomizer> customizers) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build());
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        RestClient.Builder builder = RestClient.builder().requestFactory(requestFactory);
        // Boot's customizers add tracing, so the Zipkin trace id travels to the next service
        customizers.orderedStream().forEach(c -> c.customize(builder));
        return builder;
    }
}
