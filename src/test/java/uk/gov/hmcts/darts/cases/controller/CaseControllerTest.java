package uk.gov.hmcts.darts.cases.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchRequest;
import uk.gov.hmcts.darts.cases.model.AdvancedSearchResult;
import uk.gov.hmcts.darts.cases.model.GetCasesSearchRequest;
import uk.gov.hmcts.darts.cases.service.CaseService;
import uk.gov.hmcts.darts.cases.util.RequestValidator;
import uk.gov.hmcts.darts.log.api.LogApi;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaseControllerTest {


    @Mock
    private CaseService caseService;
    @Mock
    private LogApi logApi;

    @InjectMocks
    @Spy
    private CaseController caseController;

    @Test
    void casesSearchPost_whenProvidedWithStandardData_dataShouldBeMapepdCorrectly() {
        AdvancedSearchRequest advancedSearchRequest = new AdvancedSearchRequest();
        advancedSearchRequest.setCaseNumber(" caseNumber ");
        advancedSearchRequest.setCourtroom(" courtroom ");
        advancedSearchRequest.setJudgeName(" judgeName ");
        advancedSearchRequest.setDefendantName(" defendantName ");
        advancedSearchRequest.dateFrom(LocalDate.now());
        advancedSearchRequest.dateTo(LocalDate.now().plusDays(1));
        advancedSearchRequest.setEventTextContains(" eventTextContains ");

        try (MockedStatic<RequestValidator> requestValidatorMock = Mockito.mockStatic(RequestValidator.class)) {

            List<AdvancedSearchResult> expected = List.of(mock(AdvancedSearchResult.class), mock(AdvancedSearchResult.class));
            when(caseService.advancedSearch(any())).thenReturn(expected);

            caseController.casesSearchPost(advancedSearchRequest);

            ArgumentCaptor<GetCasesSearchRequest> requestArgumentCaptor = ArgumentCaptor.forClass(GetCasesSearchRequest.class);
            verify(caseService).advancedSearch(requestArgumentCaptor.capture());
            GetCasesSearchRequest request = requestArgumentCaptor.getValue();

            assertThat(request.getCaseNumber()).isEqualTo(" caseNumber ");
            assertThat(request.getCourtroom()).isEqualTo("courtroom");
            assertThat(request.getJudgeName()).isEqualTo("judgeName");
            assertThat(request.getDefendantName()).isEqualTo("defendantName");
            assertThat(request.getDateFrom()).isEqualTo(advancedSearchRequest.getDateFrom());
            assertThat(request.getDateTo()).isEqualTo(advancedSearchRequest.getDateTo());
            assertThat(request.getEventTextContains()).isEqualTo("eventTextContains");

            requestValidatorMock.verify(() -> RequestValidator.validate(request));
        }
    }

    @Test
    void casesSearchPost_whenProvidedWithDataThatHasNulls_dataShouldBeMapepdCorrectly() {
        AdvancedSearchRequest advancedSearchRequest = new AdvancedSearchRequest();
        advancedSearchRequest.setCaseNumber(null);
        advancedSearchRequest.setCourtroom(null);
        advancedSearchRequest.setJudgeName(null);
        advancedSearchRequest.setDefendantName(null);
        advancedSearchRequest.dateFrom(null);
        advancedSearchRequest.dateTo(null);
        advancedSearchRequest.setEventTextContains(null);

        try (MockedStatic<RequestValidator> requestValidatorMock = Mockito.mockStatic(RequestValidator.class)) {

            List<AdvancedSearchResult> expected = List.of(mock(AdvancedSearchResult.class), mock(AdvancedSearchResult.class));
            when(caseService.advancedSearch(any())).thenReturn(expected);

            caseController.casesSearchPost(advancedSearchRequest);

            ArgumentCaptor<GetCasesSearchRequest> requestArgumentCaptor = ArgumentCaptor.forClass(GetCasesSearchRequest.class);
            verify(caseService).advancedSearch(requestArgumentCaptor.capture());
            GetCasesSearchRequest request = requestArgumentCaptor.getValue();

            assertThat(request.getCaseNumber()).isNull();
            assertThat(request.getCourtroom()).isNull();
            assertThat(request.getJudgeName()).isNull();
            assertThat(request.getDefendantName()).isNull();
            assertThat(request.getDateFrom()).isNull();
            assertThat(request.getDateTo()).isNull();
            assertThat(request.getEventTextContains()).isNull();

            requestValidatorMock.verify(() -> RequestValidator.validate(request));
        }
    }
}
