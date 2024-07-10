package uk.gov.hmcts.darts.testutils;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

@Component
public class TransactionalUtil {

    @Transactional()
    public void inTransaction(Runnable assertion) {
        assertion.run();
    }
}