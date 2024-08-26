package uk.gov.hmcts.darts.testutils;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.Callable;

@Component
public class TransactionalUtil {

    @Transactional()
    public void inTransaction(Runnable runnable) {
        runnable.run();
    }

    @SuppressWarnings("PMD.AvoidThrowingRawExceptionTypes")
    @Transactional
    public <T> T inTransaction(Callable<T> supplier) {
        try {
            return supplier.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
