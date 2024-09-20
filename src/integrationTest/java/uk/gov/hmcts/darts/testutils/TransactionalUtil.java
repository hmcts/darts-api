package uk.gov.hmcts.darts.testutils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

@Component
public class TransactionalUtil {

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;


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

    public void executeInTransaction(Runnable supplier) {
        executeInTransaction(() -> {
            supplier.run();
            return null;
        });
    }

    public <R> R executeInTransaction(Supplier<R> supplier) {
        if (transactionTemplate == null) {
            transactionTemplate = new TransactionTemplate(transactionManager);
        }
        return transactionTemplate.execute(status -> supplier.get());
    }
}
