package com.nis.modules.promserver.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Map;

@Data
@Component
@ConfigurationProperties(prefix = "nezha")
public class PromApi {
    private Map<String,String> promApis;
}
