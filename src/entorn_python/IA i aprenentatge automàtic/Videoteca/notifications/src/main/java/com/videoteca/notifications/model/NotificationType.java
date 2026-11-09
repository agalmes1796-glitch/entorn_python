package com.videoteca.notifications.model;

/**
 * Tipus de notificació.
 *
 * <p>Es persisteix com a text gràcies a {@code @Enumerated(EnumType.STRING)}
 * a l'entitat, perquè el valor sigui llegible directament a la taula.</p>
 */
public enum NotificationType {

    /** Notificació informativa, sense acció requerida. */
    INFO,

    /** Avís: cal prestar-hi atenció però no és crític. */
    WARNING,

    /** Error: s'ha produït un fallo o una condició crítica. */
    ERROR,

    /** Èxit: una operació s'ha completat correctament. */
    SUCCESS
}
