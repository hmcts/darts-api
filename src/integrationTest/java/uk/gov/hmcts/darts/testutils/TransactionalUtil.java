package uk.gov.hmcts.darts.testutils;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Component;

import java.util.concurrent.Callable;

@Component
public class TransactionalUtil {

    @Transactional()
    public void inTransaction(Runnable runnable) {
        runnable.run();
    }

    @Transactional
    public <T> T inTransaction(Callable<T> supplier) {
        try {
            return supplier.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
