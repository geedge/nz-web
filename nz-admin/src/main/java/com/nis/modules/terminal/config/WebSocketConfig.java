package com.nis.modules.terminal.config;

import com.nis.modules.terminal.backend.TerminalHandler;
import com.nis.modules.terminal.backend.TerminalMonitorHandler;
import com.nis.modules.terminal.interceptor.TerminalHandShakerInterceptor;
import com.nis.modules.terminal.interceptor.TerminalMonitorHandShakerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new TerminalHandler(),"/terminal.ws").setAllowedOrigins("*").addInterceptors(new TerminalHandShakerInterceptor());
        registry.addHandler(new TerminalMonitorHandler(), "/terminal/monitor.ws").setAllowedOrigins("*").addInterceptors(new TerminalMonitorHandShakerInterceptor());
    }

    @Bean
    public TerminalHandler getTerminalHandler(){
        return new TerminalHandler();
    }

    @Bean
    public TerminalMonitorHandler getTerminalMonitorHandler() {
        return new TerminalMonitorHandler();
    }
}
