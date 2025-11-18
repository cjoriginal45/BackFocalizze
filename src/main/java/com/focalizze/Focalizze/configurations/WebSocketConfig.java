package com.focalizze.Focalizze.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Este es el endpoint que el cliente (Angular) usará para conectarse al servidor WebSocket.
        // '/ws' es una convención común.
        // 'withSockJS()' es un fallback para navegadores que no soportan WebSockets nativos.
        registry.addEndpoint("/ws").setAllowedOrigins("http://localhost:4200").withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Define los prefijos de los "destinos" que el broker manejará.
        // Los clientes se suscribirán a destinos que empiecen con '/topic' o '/user'.
        registry.enableSimpleBroker("/topic", "/user");

        // Define el prefijo para los mensajes que van del cliente al servidor.
        // Ej: El cliente enviará a '/app/hello'.
        registry.setApplicationDestinationPrefixes("/app");

        // Permite enviar mensajes a un usuario específico. El destino será algo como
        // '/user/{username}/queue/notifications'.
        registry.setUserDestinationPrefix("/user");
    }
}
