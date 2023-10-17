package uk.gov.hmcts.darts.common.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.resource.ClientResources;
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

import java.time.Duration;

@Configuration
@Profile("!in-memory-caching")
@Slf4j
public class RedisConnectionConfig {

    @Value("${darts.cache.redis.host}")
    private String redisHost;
    @Value("${darts.cache.redis.port}")
    private int redisPort;

    //    @Bean
    //    public LettuceConnectionFactory redisConnectionFactory() {
    //        var conStr = "rediss://darts-stg.redis.cache.windows.net:6380";
    //        var uri = RedisURI.create(conStr);
    //        uri.setSsl(true);
    //
    //        final var redisStandaloneConfiguration = new RedisStandaloneConfiguration();
    //        redisStandaloneConfiguration.setHostName("darts-stg.redis.cache.windows.net");
    //        redisStandaloneConfiguration.setPort(6380);
    //        redisStandaloneConfiguration.setPassword("uS3LcL8QaZGwgkexggKW9yvuttHpd1wcLAzCaECdiVA%3D");
    //
    //        var clientConfigBuilder = LettuceClientConfiguration.builder();
    //        clientConfigBuilder.apply(uri);
    //        clientConfigBuilder.useSsl();
    //
    //        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfigBuilder.build());
    //    }

    @Bean
    public LettuceConnectionFactory connectionFactory() {
        final SocketOptions socketOptions = SocketOptions.builder().connectTimeout(Duration.ofSeconds(10)).build();

        final ClientResources resources = ClientResources.builder()
            .nettyCustomizer(new NettyCustomizer() {
                @Override
                public void afterBootstrapInitialized(Bootstrap bootstrap) {
                    bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPIDLE), 15);
                    bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPINTERVAL), 5);
                    bootstrap.option(NioChannelOption.of(ExtendedSocketOptions.TCP_KEEPCOUNT), 3);
                }
            })
            .build();

        final ClientOptions clientOptions = ClientOptions.builder()
            .socketOptions(socketOptions)
            .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
            .build();

        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .commandTimeout(Duration.ofSeconds(10))
            .clientOptions(clientOptions)
            .clientResources(resources)
            .useSsl()
            .build();

        RedisStandaloneConfiguration serverConfig = new RedisStandaloneConfiguration(
            "darts-stg.redis.cache.windows.net",
            6380
        );
        serverConfig.setPassword(RedisPassword.of(":uS3LcL8QaZGwgkexggKW9yvuttHpd1wcLAzCaECdiVA%3D"));

        final LettuceConnectionFactory lettuceConnectionFactory = new LettuceConnectionFactory(
            serverConfig,
            clientConfig
        );
        return lettuceConnectionFactory;
    }

}
