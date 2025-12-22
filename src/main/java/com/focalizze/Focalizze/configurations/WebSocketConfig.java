package com.focalizze.Focalizze.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Configuration for WebSocket with STOMP messaging.
 * <p>
 * Configuración para WebSocket con mensajería STOMP.
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * Registers the STOMP endpoint for client connections.
     * <p>
     * Registra el endpoint STOMP para conexiones de clientes.
     *
     * @param registry The StompEndpointRegistry. / El StompEndpointRegistry.
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // '/ws' is the connection point / '/ws' es el punto de conexión
        registry.addEndpoint("/ws").setAllowedOrigins("http://localhost:4200").withSockJS();
    }

    /**
     * Configures the message broker.
     * <p>
     * Configura el broker de mensajes.
     *
     * @param registry The MessageBrokerRegistry. / El MessageBrokerRegistry.
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Destination prefixes for server-to-client messages (Broadcasting)
        // Prefijos de destino para mensajes servidor-a-cliente (Difusión)
        registry.enableSimpleBroker("/topic", "/user");

        // Prefix for client-to-server messages
        // Prefijo para mensajes cliente-a-servidor
        registry.setApplicationDestinationPrefixes("/app");

        // Prefix for user-specific messages
        // Prefijo para mensajes específicos de usuario
        registry.setUserDestinationPrefix("/user");
    }
}
