package org.jobrad.backend.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.HttpSessionHandshakeInterceptor;

@Configuration
@EnableWebSocketMessageBroker
@EnableWebSocket
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer, WebSocketConfigurer {

    private final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);


    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/user");
        config.setApplicationDestinationPrefixes("/app");
        config.setUserDestinationPrefix("/user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/chat")
                .setAllowedOrigins("http://localhost:3000")
                .addInterceptors(new HttpSessionHandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                                   org.springframework.http.server.ServerHttpResponse response,
                                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                                   java.util.Map<String, Object> attributes) throws Exception {
                        return super.beforeHandshake(request, response, wsHandler, attributes);
                    }

                    @Override
                    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                                               org.springframework.http.server.ServerHttpResponse response,
                                               org.springframework.web.socket.WebSocketHandler wsHandler,
                                               Exception exception) {
                        if (exception != null) {
                            logger.error("WebSocket STOMP handshake error: {}", exception.getMessage());
                        }
                        super.afterHandshake(request, response, wsHandler, exception);
                    }
                })
                .withSockJS();
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ChatWebSocketHandler(), "/chat")
                .setAllowedOrigins("http://localhost:3000")
                .addInterceptors(new HttpSessionHandshakeInterceptor() {
                    @Override
                    public boolean beforeHandshake(org.springframework.http.server.ServerHttpRequest request,
                                                   org.springframework.http.server.ServerHttpResponse response,
                                                   org.springframework.web.socket.WebSocketHandler wsHandler,
                                                   java.util.Map<String, Object> attributes) throws Exception {
                        return super.beforeHandshake(request, response, wsHandler, attributes);
                    }

                    @Override
                    public void afterHandshake(org.springframework.http.server.ServerHttpRequest request,
                                               org.springframework.http.server.ServerHttpResponse response,
                                               org.springframework.web.socket.WebSocketHandler wsHandler,
                                               Exception exception) {
                        if (exception != null) {
                            logger.error("WebSocket RAW handshake error: {}", exception.getMessage());
                        }
                        super.afterHandshake(request, response, wsHandler, exception);
                    }
                });
    }
}