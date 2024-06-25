package uk.gov.hmcts.darts.testutils;

import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.jpa.EntityManagerHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@RequiredArgsConstructor
@SuppressWarnings("PMD.CloseResource")
public class OpenInViewUtil {

    private final EntityManagerFactory entityManagerFactory;


    public void openEntityManager() {
        var em = this.entityManagerFactory.createEntityManager();
        var emHolder = new EntityManagerHolder(em);
        TransactionSynchronizationManager.bindResource(this.entityManagerFactory, emHolder);
    }

    public void closeEntityManager() {
        var emHolder = (EntityManagerHolder) TransactionSynchronizationManager.unbindResource(this.entityManagerFactory);
        var em = emHolder.getEntityManager();
        if (em.isOpen()) {
            em.close();
        }
    }
}