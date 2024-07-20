package uk.gov.hmcts.darts.testutils;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Slf4j
public class ClearDatabaseExtension implements BeforeAllCallback, BeforeEachCallback {

    @Override public void beforeEach(ExtensionContext extensionContext) throws Exception {
        Flyway flyway = SpringExtension.getApplicationContext(extensionContext).getBean(Flyway.class);
        flyway.clean();
        flyway.migrate();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) {
//        Flyway flyway = SpringExtension.getApplicationContext(extensionContext).getBean(Flyway.class);
//        flyway.clean();
//        ApplicationContext springContext = SpringExtension.getApplicationContext(extensionContext);
        log.info("");

    }

    public void truncateTables () {

//        EntityManager em = emf.createEntityManager();
//
//        em.getTransaction().begin();
//        final Query query = em
//            .createNativeQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = 'darts'");
//        final List result = query.getResultList();
//        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate();
//        for (Object tableName : result) {
//            if (!excludedTables.contains(tableName.toString())) {
//                em.createNativeQuery("TRUNCATE TABLE darts." + tableName + " RESTART IDENTITY").executeUpdate();
//            }
//        }
//        em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate();
//        em.getTransaction().commit();
    }
}
