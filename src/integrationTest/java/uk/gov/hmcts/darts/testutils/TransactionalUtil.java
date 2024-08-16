package uk.gov.hmcts.darts.testutils;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
public class TransactionalUtil {

    @Transactional()
    public void inTransaction(Runnable assertion) {
        assertion.run();
    }

    @Transactional
    public <T> T inTransaction(Callable<T> callable) {
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
