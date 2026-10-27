package com.campus.config;

import java.time.Duration;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class InfrastructureConfig {

  @Bean
  Queue operationLogQueue() {
    return new Queue("campus.operation-log", true);
  }

  @Bean
  RedisCacheManager cacheManager(
    org.springframework.data.redis.connection.RedisConnectionFactory connectionFactory
  ) {
    return RedisCacheManager.builder(connectionFactory)
      .cacheDefaults(
        RedisCacheConfiguration.defaultCacheConfig()
          .entryTtl(Duration.ofMinutes(5))
          .serializeValuesWith(
            RedisSerializationContext.SerializationPair.fromSerializer(
              new GenericJackson2JsonRedisSerializer()
            )
          )
      )
      .build();
  }

  @Bean
  Jackson2JsonMessageConverter rabbitMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  RabbitTemplate rabbitTemplate(
    ConnectionFactory connectionFactory,
    Jackson2JsonMessageConverter converter
  ) {
    var template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(converter);
    template.setMandatory(true);
    return template;
  }
}
