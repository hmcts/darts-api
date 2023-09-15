package uk.gov.hmcts.darts.authorisation.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.when;

@Transactional
class AuthorisationImplTest extends IntegrationBase {

    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    private AuthorisationStub authorisationStub;

    @Autowired
    private Authorisation authorisationToTest;


    @BeforeEach
    void beforeEach() {
        authorisationStub.givenTestSchema();
        when(mockUserIdentity.getEmailAddress()).thenReturn(authorisationStub.getTestUser().getEmailAddress());
    }

    @Test
    void authoriseByCaseId() {
        assertDoesNotThrow(() -> authorisationToTest.authoriseByCaseId(
            authorisationStub.getCourtCaseEntity().getId()));
    }

    @Test
    void authoriseByHearingId() {
        assertDoesNotThrow(() -> authorisationToTest.authoriseByHearingId(
            authorisationStub.getHearingEntity().getId()));
    }

    @Test
    void authoriseByMediaRequestId() {
        assertDoesNotThrow(() -> authorisationToTest.authoriseByMediaRequestId(
            authorisationStub.getMediaRequestEntity().getId()));
    }

    @Test
    void authoriseByMediaId() {
        assertDoesNotThrow(() -> authorisationToTest.authoriseByMediaId(
            authorisationStub.getMediaEntity().getId()));
    }

    @Test
    void authoriseByTranscriptionId() {
        assertDoesNotThrow(() -> authorisationToTest.authoriseByTranscriptionId(
            authorisationStub.getTranscriptionEntity().getId()));
    }

}
