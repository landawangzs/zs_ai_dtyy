package com.zs.zsaidtyy.config;


import com.zhipu.oapi.ClientV4;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@ConfigurationProperties(prefix = "ai")
@Data
public class AiConfig {
    /**
     * ai 配置key
     */
    private String apiKey;
    @Bean
    public ClientV4 getClientV4(){
        return new ClientV4.Builder(apiKey).build();
    }

}
