package com.focalizze.Focalizze.models;

/**
 * Enumeration defining the security roles within the application.
 * Controls access to specific endpoints and features.
 * <p>
 * Enumeración que define los roles de seguridad dentro de la aplicación.
 * Controla el acceso a endpoints y características específicas.
 */
public enum UserRole {
    /**
     * Standard user with access to basic features (posting, liking, commenting).
     * Usuario estándar con acceso a características básicas (publicar, dar like, comentar).
     */
    USER,

    /**
     * Administrator with elevated privileges (moderation, banning, system settings).
     * Administrador con privilegios elevados (moderación, baneo, configuración del sistema).
     */
    ADMIN
}
