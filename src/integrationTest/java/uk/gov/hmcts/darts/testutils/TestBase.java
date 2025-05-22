package uk.gov.hmcts.darts.testutils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.junit.jupiter.api.BeforeEach;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiError;
import uk.gov.hmcts.darts.testutils.stubs.DartsDatabaseStub;
import uk.gov.hmcts.darts.testutils.stubs.DartsPersistence;

import java.io.UnsupportedEncodingException;

import static org.assertj.core.api.Assertions.assertThat;

@Getter
@SuppressWarnings("PMD.TestClassWithoutTestCases")//This is a test support class
public class TestBase {

    @Autowired
    protected DartsDatabaseStub dartsDatabase;

    @Autowired
    protected DartsPersistence dartsPersistence;

    @Autowired
    protected OpenInViewUtil openInViewUtil;

    @Autowired
    protected TransactionalUtil transactionalUtil;
    @Autowired
    protected ObjectMapper objectMapper;
    @Autowired
    protected GivenBuilder givenBuilder;

    @Autowired
    @Qualifier("inMemoryCacheManager")
    private CacheManager inMemoryCacheManager;

    @BeforeEach
    void beforeEach() {
        dartsDatabase.clearDb();
        evictCache();
    }

    protected void evictCache() {
        inMemoryCacheManager.getCacheNames().stream()
            .forEach(cacheName -> inMemoryCacheManager.getCache(cacheName).clear());
    }

    public void assertStandardErrorJsonResponse(MvcResult mvcResult, DartsApiError dartsApiError) throws UnsupportedEncodingException {
        String expectedJson = """
            {
              "type": "<TYPE>",
              "title": "<TITLE>",
              "status": <STATUS>
            }
            """
            .replace("<TYPE>", dartsApiError.getType())
            .replace("<TITLE>", dartsApiError.getTitle())
            .replace("<STATUS>", String.valueOf(dartsApiError.getHttpStatus().value()));
        String actualJson = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(dartsApiError.getHttpStatus().value());
    }

    public void assertStandardErrorJsonResponse(MvcResult mvcResult, DartsApiError dartsApiError, String detail) throws UnsupportedEncodingException {
        String expectedJson = """
            {
              "type": "<TYPE>",
              "title": "<TITLE>",
              "status": <STATUS>,
              "detail": "<DETAIL>"
            }
            """
            .replace("<TYPE>", dartsApiError.getType())
            .replace("<TITLE>", dartsApiError.getTitle())
            .replace("<STATUS>", String.valueOf(dartsApiError.getHttpStatus().value()))
            .replace("<DETAIL>", detail);
        String actualJson = mvcResult.getResponse().getContentAsString();
        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
        assertThat(mvcResult.getResponse().getStatus()).isEqualTo(dartsApiError.getHttpStatus().value());
    }

    protected UserAccountEntity anAuthenticatedUserFor(String userEmail) {
        return GivenBuilder.anAuthenticatedUserFor(userEmail, dartsDatabase.getUserAccountRepository());
    }


    protected void clearEntityManagerCache() {
        if (hasTransaction()) {
            dartsDatabase.getEntityManager().flush();//Commit the transaction to ensure all changes are saved
        }
        dartsDatabase.getEntityManager().clear();//Clear the entity manager to force a new query
    }

    protected boolean hasTransaction() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }
}
