package com.duhao.security.checkinapp.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    /**
     * 声明 RestTemplate Bean，供全局注入使用。
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
