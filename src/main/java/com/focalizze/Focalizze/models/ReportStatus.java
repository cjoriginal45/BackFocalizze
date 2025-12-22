package com.focalizze.Focalizze.models;

/**
 * Enumeration representing the lifecycle states of a moderation report.
 * <p>
 * Enumeración que representa los estados del ciclo de vida de un reporte de moderación.
 */
public enum ReportStatus {
    /**
     * The report has been created but not yet reviewed by an admin.
     * El reporte ha sido creado pero aún no ha sido revisado por un administrador.
     */
    PENDING,
    /**
     * An admin has seen the report and is currently investigating.
     * Un administrador ha visto el reporte y está investigando actualmente.
     */
    REVIEWED,

    /**
     * The report was deemed invalid or required no action.
     * El reporte se consideró inválido o no requirió ninguna acción.
     */
    DISMISSED,

    /**
     * Punitive or corrective measures have been applied (e.g., ban, delete).
     * Se han aplicado medidas punitivas o correctivas (ej. baneo, eliminación).
     */
    ACTION_TAKEN
}
