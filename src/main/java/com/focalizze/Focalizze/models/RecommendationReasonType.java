package com.focalizze.Focalizze.models;

/**
 * Enumeration defining why a thread was recommended to a user.
 * <p>
 * Enumeración que define por qué se recomendó un hilo a un usuario.
 */
public enum RecommendationReasonType {
    /**
     * Recommended because the user follows the thread's category.
     * Recomendado porque el usuario sigue la categoría del hilo.
     */
    CATEGORY_INTEREST,
    /**
     * Recommended based on engagement metrics or social circle activity.
     * Recomendado basado en métricas de participación o actividad del círculo social.
     */
    SOCIAL_PROOF
}
