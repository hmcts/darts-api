package uk.gov.hmcts.darts.common.util;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

public class ReprovisionDatabaseBeforeEachExtension implements BeforeEachCallback {
    @Override public void beforeEach(ExtensionContext extensionContext) {
        Flyway flyway = SpringExtension.getApplicationContext(extensionContext)
            .getBean(Flyway.class);
        flyway.clean();
        flyway.migrate();
    }
}
