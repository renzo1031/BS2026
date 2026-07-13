package com.campusbuddies.messaging;

import com.campusbuddies.config.AppProperties;
import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final ChatWebSocketHandler handler;
    private final TicketHandshakeInterceptor ticketInterceptor;
    private final AppProperties.Cors cors;

    public WebSocketConfig(ChatWebSocketHandler handler, TicketHandshakeInterceptor ticketInterceptor,
                           AppProperties.Cors cors) {
        this.handler = handler;
        this.ticketInterceptor = ticketInterceptor;
        this.cors = cors;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = Arrays.stream(cors.allowedOrigins().split(","))
                .map(String::trim).filter(value -> !value.isEmpty()).toArray(String[]::new);
        registry.addHandler(handler, "/ws").addInterceptors(ticketInterceptor).setAllowedOrigins(origins);
    }
}
