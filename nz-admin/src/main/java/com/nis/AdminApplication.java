
package com.nis;

import com.nis.modules.terminal.config.TerminalHandler;

import io.micrometer.core.instrument.MeterRegistry;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;


@SpringBootApplication
public class AdminApplication{
	public static void main(String[] args) {

		ConfigurableApplicationContext run = SpringApplication.run(AdminApplication.class, args);
		TerminalHandler.setApplicationContext(run);
	}
	
	@Bean
    MeterRegistryCustomizer<MeterRegistry> configurer(@Value("Nezha") String applicationName) {
        return (registry) -> registry.config().commonTags("application", applicationName);
    }
	 
}