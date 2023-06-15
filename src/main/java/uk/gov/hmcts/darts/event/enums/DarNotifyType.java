package uk.gov.hmcts.darts.event.enums;

public enum DarNotifyType {

    START_RECORDING("1"),
    STOP_RECORDING("2"),
    CASE_UPDATE("3");

    private final String notificationType;

    DarNotifyType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getNotificationType() {
        return notificationType;
    }

}
