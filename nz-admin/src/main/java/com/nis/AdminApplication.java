
package com.nis;

import com.nis.modules.romote.websocket.TerminalHandler;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;


@SpringBootApplication
public class AdminApplication {
	public static void main(String[] args) {

		ConfigurableApplicationContext run = SpringApplication.run(AdminApplication.class, args);
		TerminalHandler.setApplicationContext(run);
	}
}
