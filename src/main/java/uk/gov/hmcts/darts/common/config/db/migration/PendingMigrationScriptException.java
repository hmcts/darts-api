package uk.gov.hmcts.darts.common.config.db.migration;

import java.io.Serial;

public class PendingMigrationScriptException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    public PendingMigrationScriptException(String script) {
        super("Found migration not yet applied: " + script);
    }


}
