package uk.gov.hmcts.darts.testutils;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class TransactionalAssert {

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Transactional
    public void assertWithTransaction(Runnable assertion) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.execute((status) -> {
            assertion.run();
            return null;
        });
    }
}
