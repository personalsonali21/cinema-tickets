package uk.gov.dwp.uc.pairtest.constants;

public enum ErrorMessage {
    INVALID_ACCOUNT("ERR001", "Invalid account id"),
    EMPTY_REQUEST("ERR002", "At least one ticket must be requested"),
    NEGATIVE_TICKET("ERR003", "Number of tickets cannot be negative"),
    MAX_TICKETS_EXCEEDED("ERR004", "More than 25 tickets cannot be purchased at a time"),
    CHILD_INFANT_WITHOUT_ADULT("ERR005", "Child or Infant tickets require at least one Adult ticket");

    private final String code;
    private final String message;

    ErrorMessage(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String code() {
        return code;
    }

    public String message() {
        return message;
    }

    @Override
    public String toString() {
        return code + ": " + message;
    }
}
