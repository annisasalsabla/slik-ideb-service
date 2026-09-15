package co.id.skyworx.slikideb.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * STOMP over WebSocket configuration supporting both SockJS and pure WebSocket
 * clients.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // SockJS fallback for browser compatibility
        registry.addEndpoint("/ws-ideb")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        // Native STOMP endpoint for Hoppscotch and CLI tools
        registry.addEndpoint("/ws-ideb")
                .setAllowedOriginPatterns("*");
    }
}