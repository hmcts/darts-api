package uk.gov.hmcts.darts.common.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.ClientOptions.DisconnectedBehavior;
import io.lettuce.core.RedisURI;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.internal.HostAndPort;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DnsResolvers;
import io.lettuce.core.resource.MappingSocketAddressResolver;
import io.lettuce.core.resource.NettyCustomizer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.socket.nio.NioChannelOption;
import jdk.net.ExtendedSocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.function.UnaryOperator;

import static java.nio.charset.Charset.defaultCharset;
import static java.time.Duration.ofSeconds;

@Configuration
@Profile("!in-memory-caching")
@Slf4j
public class RedisConnectionConfig {

    @Value("${darts.redis.connection-string}")
    private String redisConnectionString;

    @Value("${darts.redis.ssl-enabled}")
    private boolean sslEnabled;

    @Bean
    public LettuceConnectionFactory connectionFactory() {
        var redisConnectionProperties = redisConnectionPropertiesFrom(redisConnectionString);
        var mappingSocketAddressResolver = MappingSocketAddressResolver.create(
            DnsResolvers.JVM_DEFAULT,
            getHostAndPortMappingFunctionFor(redisConnectionProperties.host())
        );

        var clientResources = ClientResources.builder()
            .nettyCustomizer(new CustomNettyConfig())
            .socketAddressResolver(mappingSocketAddressResolver)
            .build();

        var socketOptions = SocketOptions.builder()
            .connectTimeout(ofSeconds(20))
            .build();

        var clientOptions = ClientOptions.builder()
            .timeoutOptions(TimeoutOptions.enabled(ofSeconds(20)))
            .socketOptions(socketOptions)
            .disconnectedBehavior(DisconnectedBehavior.REJECT_COMMANDS)
            .build();

        var clientConfigurationBuilder = LettuceClientConfiguration.builder();
        clientConfigurationBuilder
            .commandTimeout(ofSeconds(20))
            .clientOptions(clientOptions)
            .clientResources(clientResources);
        if (sslEnabled) {
            clientConfigurationBuilder.useSsl();
        }

        var redisConfig = new RedisStandaloneConfiguration(
            redisConnectionProperties.host(),
            redisConnectionProperties.port()
        );
        redisConfig.setPassword(RedisPassword.of(redisConnectionProperties.password()));

        return new LettuceConnectionFactory(redisConfig, clientConfigurationBuilder.build());
    }

    private UnaryOperator<HostAndPort> getHostAndPortMappingFunctionFor(String host) {
        return hostAndPort -> {
            var addresses = new InetAddress[0];
            try {
                addresses = DnsResolvers.JVM_DEFAULT.resolve(host);
            } catch (UnknownHostException unknownHostException) {
                log.error("Failed to resolve: " + host, unknownHostException);
            }

            var hostIp = addresses[0].getHostAddress();
            var finalAddress = hostAndPort;
            if (hostAndPort.hostText.equals(hostIp)) {
                finalAddress = HostAndPort.of(host, hostAndPort.getPort());
            }

            return finalAddress;
        };
    }

    private static class CustomNettyConfig implements NettyCustomizer {

        @Override
        public void afterBootstrapInitialized(Bootstrap bootstrap) {
            bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE), 15);
            bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPINTERVAL), 5);
            bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPCOUNT), 3);
        }
    }

    static RedisConnectionProperties redisConnectionPropertiesFrom(String redisConnectionString) {
        var redisUri = RedisURI.create(redisConnectionString);

        var redisPassword = RedisPassword.of(redisUri.getPassword());
        char[] decodedPasswordChars = {};
        if (redisPassword.isPresent()) {
            var encodedPassword = redisPassword.get();
            var decodedPasswordString = URLDecoder.decode(String.valueOf(encodedPassword), defaultCharset());
            decodedPasswordChars = decodedPasswordString.toCharArray();
        }

        return new RedisConnectionProperties(
            redisUri.getUsername(),
            decodedPasswordChars,
            redisUri.getHost(),
            redisUri.getPort());
    }

    public record RedisConnectionProperties(String username, char[] password, String host, Integer port) {

    }
}
