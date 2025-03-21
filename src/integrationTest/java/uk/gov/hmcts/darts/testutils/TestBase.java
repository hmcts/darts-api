package uk.gov.hmcts.darts.testutils;

import lombok.Getter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.test.web.servlet.MvcResult;
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
    @Qualifier("inMemoryCacheManager")
    private CacheManager inMemoryCacheManager;

    @BeforeEach
    void clearDb() {
        dartsDatabase.clearDb();
    }

    @AfterEach
    void clearTestData() {
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

    protected void anAuthenticatedUserFor(String userEmail) {
        GivenBuilder.anAuthenticatedUserFor(userEmail, dartsDatabase.getUserAccountRepository());
    }
}
