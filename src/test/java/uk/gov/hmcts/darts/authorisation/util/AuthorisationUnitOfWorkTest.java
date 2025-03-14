package uk.gov.hmcts.darts.authorisation.util;


import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.authorisation.component.ControllerAuthorisation;
import uk.gov.hmcts.darts.authorisation.enums.ContextIdEnum;
import uk.gov.hmcts.darts.authorisation.exception.AuthorisationError;
import uk.gov.hmcts.darts.cases.exception.CaseApiError;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

class AuthorisationUnitOfWorkTest {
    @Test
    void testAuthorisationForTransaction() throws Exception {

        ControllerAuthorisation authorisation = Mockito.mock(ControllerAuthorisation.class);
        Mockito.when(authorisation.getContextId()).thenReturn(ContextIdEnum.TRANSCRIPTION_ID);

        List<ControllerAuthorisation> authorisationList = new ArrayList<>();
        authorisationList.add(authorisation);

        List<IdSource> idobject = new ArrayList<>();
        idobject.add(new IdSource());

        Runnable runnable = Mockito.mock(Runnable.class);
        SecurityRoleEnum[] rolesArr = {SecurityRoleEnum.APPROVER};

        AuthorisationUnitOfWork unitOfWork = new AuthorisationUnitOfWork(authorisationList);
        unitOfWork.authoriseWithIdsForTranscription(idobject, (e) -> e.getId(), rolesArr, runnable);

        Mockito.verify(runnable).run();
        Mockito.verify(authorisation).checkAuthorisation(Mockito.argThat(new IdSupplierMatcher("200")), Mockito.notNull());
    }

    @Test
    void testAuthorisationWithId() throws Exception {

        ControllerAuthorisation authorisation = Mockito.mock(ControllerAuthorisation.class);
        Mockito.when(authorisation.getContextId()).thenReturn(ContextIdEnum.HEARING_ID);

        List<ControllerAuthorisation> authorisationList = new ArrayList<>();
        authorisationList.add(authorisation);

        List<IdSource> idobject = new ArrayList<>();
        idobject.add(new IdSource());

        Runnable runnable = Mockito.mock(Runnable.class);
        SecurityRoleEnum[] rolesArr = {SecurityRoleEnum.APPROVER};

        AuthorisationUnitOfWork unitOfWork = new AuthorisationUnitOfWork(authorisationList);
        unitOfWork.authoriseWithIds(idobject, (e) -> e.getId(), ContextIdEnum.HEARING_ID, rolesArr, runnable, true);

        Mockito.verify(runnable).run();
        Mockito.verify(authorisation).checkAuthorisation(Mockito.argThat(new IdSupplierMatcher("200")), Mockito.notNull());
    }

    @Test
    void testAuthorisationThrowForbidden() throws Exception {

        ControllerAuthorisation authorisation = new ControllerAuthorisation() {
            @Override
            public ContextIdEnum getContextId() {
                return ContextIdEnum.HEARING_ID;
            }

            @Override
            public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
                // Empty method
            }

            @Override
            public void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles) {
                throw new DartsApiException(AuthorisationError.USER_NOT_AUTHORISED_FOR_COURTHOUSE);
            }

            @Override
            public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {
                // Empty method
            }
        };

        List<ControllerAuthorisation> authorisationList = new ArrayList<>();
        authorisationList.add(authorisation);

        List<IdSource> idobject = new ArrayList<>();
        idobject.add(new IdSource());

        Runnable runnable = Mockito.mock(Runnable.class);
        SecurityRoleEnum[] rolesArr = {SecurityRoleEnum.APPROVER};

        AuthorisationUnitOfWork unitOfWork = new AuthorisationUnitOfWork(authorisationList);

        Assertions.assertThrows(DartsApiException.class, () ->
            unitOfWork.authoriseWithIds(idobject, (e) -> e.getId(), ContextIdEnum.HEARING_ID, rolesArr, runnable, true));

        Mockito.verifyNoInteractions(runnable);
    }

    @Test
    void testAuthorisationThrowNoSuppressDataException() throws Exception {

        ControllerAuthorisation authorisation = new ControllerAuthorisation() {
            @Override
            public ContextIdEnum getContextId() {
                return ContextIdEnum.HEARING_ID;
            }

            @Override
            public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
            }

            @Override
            public void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles) {
                throw new DartsApiException(CaseApiError.CASE_NOT_FOUND);
            }

            @Override
            public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {

            }
        };

        List<ControllerAuthorisation> authorisationList = new ArrayList<>();
        authorisationList.add(authorisation);

        List<IdSource> idobject = new ArrayList<>();
        idobject.add(new IdSource());

        Runnable runnable = Mockito.mock(Runnable.class);
        SecurityRoleEnum[] rolesArr = {SecurityRoleEnum.APPROVER};

        AuthorisationUnitOfWork unitOfWork = new AuthorisationUnitOfWork(authorisationList);

        Assertions.assertThrows(DartsApiException.class, () ->
            unitOfWork.authoriseWithIds(idobject, (e) -> e.getId(), ContextIdEnum.HEARING_ID, rolesArr, runnable, false));

        Mockito.verifyNoInteractions(runnable);
    }

    @Test
    void testAuthorisationThrowSuppressDataException() throws Exception {

        ControllerAuthorisation authorisation = new ControllerAuthorisation() {
            @Override
            public ContextIdEnum getContextId() {
                return ContextIdEnum.HEARING_ID;
            }

            @Override
            public void checkAuthorisation(HttpServletRequest request, Set<SecurityRoleEnum> roles) {
            }

            @Override
            public void checkAuthorisation(Supplier<Optional<String>> idToAuthorise, Set<SecurityRoleEnum> roles) {
                throw new DartsApiException(CaseApiError.CASE_NOT_FOUND);
            }

            @Override
            public void checkAuthorisation(JsonNode jsonNode, Set<SecurityRoleEnum> roles) {

            }
        };

        List<ControllerAuthorisation> authorisationList = new ArrayList<>();
        authorisationList.add(authorisation);

        List<IdSource> idobject = new ArrayList<>();
        idobject.add(new IdSource());

        Runnable runnable = Mockito.mock(Runnable.class);
        SecurityRoleEnum[] rolesArr = {SecurityRoleEnum.APPROVER};

        AuthorisationUnitOfWork unitOfWork = new AuthorisationUnitOfWork(authorisationList);

        unitOfWork.authoriseWithIds(idobject, (e) -> e.getId(), ContextIdEnum.HEARING_ID, rolesArr, runnable, true);

        Mockito.verify(runnable).run();
    }

    static class IdSource {
        public String getId() {
            return "200";
        }
    }

    class IdSupplierMatcher implements ArgumentMatcher<Supplier<Optional<String>>> {

        private final String id;

        public IdSupplierMatcher(String id) {
            this.id = id;
        }

        @Override
        public boolean matches(Supplier<Optional<String>> supplierCompare) {
            return supplierCompare.get().get().equals(id);
        }
    }
}
