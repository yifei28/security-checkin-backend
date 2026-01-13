package com.duhao.security.checkinapp;

import com.duhao.security.checkinapp.service.DelayedTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class SecurityCheckinApplication {

    private static final Logger logger = LoggerFactory.getLogger(SecurityCheckinApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(SecurityCheckinApplication.class, args);
    }

    /**
     * 应用启动后检查 Redis 连接和队列状态
     */
    @Bean
    public ApplicationRunner redisStatusChecker(StringRedisTemplate redisTemplate, DelayedTaskService delayedTaskService) {
        return args -> {
            try {
                // 测试 Redis 连接
                String pong = redisTemplate.getConnectionFactory().getConnection().ping();
                logger.info("[启动检查] Redis 连接正常: {}", pong);

                // 检查队列状态
                Long sessionTimeoutSize = delayedTaskService.getQueueSize(DelayedTaskService.KEY_SESSION_TIMEOUT);
                Long spotCheckTriggerSize = delayedTaskService.getQueueSize(DelayedTaskService.KEY_SPOTCHECK_TRIGGER);
                Long spotCheckTimeoutSize = delayedTaskService.getQueueSize(DelayedTaskService.KEY_SPOTCHECK_TIMEOUT);

                logger.info("[启动检查] 延迟队列状态: timeout:session={}, spotcheck:trigger={}, spotcheck:timeout={}",
                        sessionTimeoutSize, spotCheckTriggerSize, spotCheckTimeoutSize);

            } catch (Exception e) {
                logger.error("[启动检查] Redis 连接失败!", e);
            }
        };
    }

}
