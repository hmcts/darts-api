package uk.gov.hmcts.darts.authorisation.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.Authorisation;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.authorisation.exception.AuthorisationError.USER_NOT_AUTHORISED_FOR_COURTHOUSE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.APPROVER;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.JUDGE;
import static uk.gov.hmcts.darts.common.enums.SecurityRoleEnum.REQUESTER;

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
            authorisationStub.getCourtCaseEntity().getId(), Set.of(APPROVER, REQUESTER)));
    }

    @Test
    void authoriseByCaseIdShouldThrowException() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> authorisationToTest.authoriseByCaseId(
                authorisationStub.getCourtCaseEntity().getId(), Set.of(JUDGE))
        );

        assertEquals(USER_NOT_AUTHORISED_FOR_COURTHOUSE.getTitle(), exception.getMessage());
        assertEquals(USER_NOT_AUTHORISED_FOR_COURTHOUSE, exception.getError());
    }

    @Test
    void authoriseByHearingId() {
        assertDoesNotThrow(() -> authorisationToTest.authoriseByHearingId(
            authorisationStub.getHearingEntity().getId(), Set.of(APPROVER, REQUESTER)));
    }

    @Test
    void authoriseByHearingIdShouldThrowException() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> authorisationToTest.authoriseByHearingId(
                authorisationStub.getHearingEntity().getId(), Set.of(JUDGE))
        );

        assertEquals(USER_NOT_AUTHORISED_FOR_COURTHOUSE.getTitle(), exception.getMessage());
        assertEquals(USER_NOT_AUTHORISED_FOR_COURTHOUSE, exception.getError());
    }

    @Test
    void authoriseByMediaRequestId() {
        assertDoesNotThrow(() -> authorisationToTest.authoriseByMediaRequestId(
            authorisationStub.getMediaRequestEntity().getId(), Set.of(APPROVER, REQUESTER)));
    }

    @Test
    void authoriseByMediaRequestIdShouldThrowException() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> authorisationToTest.authoriseByMediaRequestId(
                authorisationStub.getMediaRequestEntity().getId(), Set.of(JUDGE))
        );

        assertEquals(USER_NOT_AUTHORISED_FOR_COURTHOUSE.getTitle(), exception.getMessage());
        assertEquals(USER_NOT_AUTHORISED_FOR_COURTHOUSE, exception.getError());
    }

    @Test
    void authoriseByMediaId() {
        assertDoesNotThrow(() -> authorisationToTest.authoriseByMediaId(
            authorisationStub.getMediaEntity().getId(), Set.of(APPROVER, REQUESTER)));
    }

    @Test
    void authoriseByMediaIdShouldThrowException() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> authorisationToTest.authoriseByMediaId(
                authorisationStub.getMediaEntity().getId(), Set.of(JUDGE))
        );

        assertEquals(USER_NOT_AUTHORISED_FOR_COURTHOUSE.getTitle(), exception.getMessage());
        assertEquals(USER_NOT_AUTHORISED_FOR_COURTHOUSE, exception.getError());
    }

    @Test
    void authoriseByTranscriptionId() {
        assertDoesNotThrow(() -> authorisationToTest.authoriseByTranscriptionId(
            authorisationStub.getTranscriptionEntity().getId(), Set.of(APPROVER, REQUESTER)));
    }

    @Test
    void authoriseByTranscriptionIdShouldThrowException() {
        var exception = assertThrows(
            DartsApiException.class,
            () -> authorisationToTest.authoriseByTranscriptionId(
                authorisationStub.getTranscriptionEntity().getId(), Set.of(JUDGE))
        );

        assertEquals(USER_NOT_AUTHORISED_FOR_COURTHOUSE.getTitle(), exception.getMessage());
        assertEquals(USER_NOT_AUTHORISED_FOR_COURTHOUSE, exception.getError());
    }

}
