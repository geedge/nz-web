package com.nis.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "spring.flyway")
public class FlywayProperties {

    private boolean enabled;

    private String encoding;

    private String sqlMigrationPrefix;

    private String sqlMigrationSeparator;

    private boolean baselineOnMigrate;

    private String locations;

    private boolean cleanDisabled;

    private boolean validateOnMigrate;

    private boolean placeholderReplacement;

    public boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}