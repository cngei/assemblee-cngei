package it.cngei.assemblee.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;

@Configuration
public class RedisConfiguration {
  @Value("${redisHost}")
  private String redisHost;

  @Bean("presenzeFactory")
  @Primary
  LettuceConnectionFactory presenzeFactory() {
    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost, 6379);
    var factory = new LettuceConnectionFactory(redisStandaloneConfiguration);
    factory.setDatabase(0);
    return factory;
  }

  @Bean("votiFactory")
  LettuceConnectionFactory votiFactory() {
    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost, 6379);
    var factory = new LettuceConnectionFactory(redisStandaloneConfiguration);
    factory.setDatabase(1);
    return factory;
  }

  @Bean("keysFactory")
  LettuceConnectionFactory keysFactory() {
    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration(redisHost, 6379);
    var factory = new LettuceConnectionFactory(redisStandaloneConfiguration);
    factory.setDatabase(2);
    return factory;
  }

  @Bean("presenzeTemplate")
  public RedisTemplate<Long, Long> presenzeTemplate() {
    RedisTemplate<Long, Long> template = new RedisTemplate<>();
    template.setValueSerializer(new GenericToStringSerializer<>(Long.class));
    template.setConnectionFactory(presenzeFactory());
    return template;
  }

  @Bean("votiTemplate")
  public RedisTemplate<Long, Long> votiTemplate() {
    RedisTemplate<Long, Long> template = new RedisTemplate<>();
    template.setConnectionFactory(votiFactory());
    return template;
  }

  @Bean("keysTemplate")
  public RedisTemplate<Long, Object> keysTemplate() {
    RedisTemplate<Long, Object> template = new RedisTemplate<>();
    template.setConnectionFactory(keysFactory());
    return template;
  }
}
