package pe.waterdog.event;

/**
 * Represents the Priority of an event.
 * Default event priority, if not specific otherwise, is NORMAL.
 * HIGHEST is called first,
 * LOWEST is called last.
 */
public enum EventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST
}
