package uk.gov.hmcts.darts;

public enum DartsMode {
    ATS_MODE("ATS_MODE");

    private String mode;

    DartsMode(String mode) {
        this.mode = mode;
    }

    public String getModeStr() {
        return mode;
    }
}