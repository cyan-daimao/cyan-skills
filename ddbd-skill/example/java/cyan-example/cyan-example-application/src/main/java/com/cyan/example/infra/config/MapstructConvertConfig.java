package com.cyan.example.infra.config;

import com.cyan.arch.common.mapstruct.MapstructConvert;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MapstructConvertConfig {

    @Bean
    public MapstructConvert mapstructConvert() {
        return new MapstructConvert();
    }
}
