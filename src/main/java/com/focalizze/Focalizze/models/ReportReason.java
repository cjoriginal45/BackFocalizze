package com.focalizze.Focalizze.models;

/**
 * Enumeration of predefined reasons for reporting content or users.
 * <p>
 * Enumeración de razones predefinidas para reportar contenido o usuarios.
 */
public enum ReportReason {
    /**
     * Unwanted or repetitive content.
     * Contenido no deseado o repetitivo.
     */
    SPAM,

    /**
     * Targeted harassment or hate speech.
     * Acoso dirigido o discurso de odio.
     */
    HARASSMENT_OR_HATE,

    /**
     * Content not suitable for the platform (e.g., NSFW).
     * Contenido no adecuado para la plataforma.
     */
    INAPPROPRIATE_CONTENT,

    /**
     * Impersonation or bot accounts.
     * Suplantación de identidad o cuentas bot.
     */
    FAKE_ACCOUNT,

    /**
     * Any other reason not listed above.
     * Cualquier otra razón no listada arriba.
     */
    OTHER
}