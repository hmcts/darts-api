package uk.gov.hmcts.darts.audio.auditing;

public class AuditExecutor {
    public void run(Runnable runnable) {
        runnable.run();
    }
}
