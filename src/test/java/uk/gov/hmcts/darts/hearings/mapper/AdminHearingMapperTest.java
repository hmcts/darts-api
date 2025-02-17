package uk.gov.hmcts.darts.hearings.mapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.hearings.model.HearingsResponse;
import uk.gov.hmcts.darts.hearings.model.HearingsResponseCase;
import uk.gov.hmcts.darts.hearings.model.HearingsResponseHearing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

class AdminHearingMapperTest {
    MockedStatic<HearingCommonMapper> hearingCommonMapperMockedStatic;
    MockedStatic<AdminHearingMapper> adminHearingMapperMockedStatic;

    @BeforeEach
    void beforeEach() {
        hearingCommonMapperMockedStatic = Mockito.mockStatic(HearingCommonMapper.class, Mockito.CALLS_REAL_METHODS);
        adminHearingMapperMockedStatic = Mockito.mockStatic(AdminHearingMapper.class, Mockito.CALLS_REAL_METHODS);
    }

    @AfterEach
    void afterEach() {
        hearingCommonMapperMockedStatic.close();
        adminHearingMapperMockedStatic.close();
    }

    @Test
    void mapToHearingsResponse_hasNullInput_shouldReturnNull() {
        assertThat(AdminHearingMapper.mapToHearingsResponse(null)).isNull();
    }

    @Test
    void mapToHearingsResponse_hasData_shouldReturnMappedValues() {
        CourtCaseEntity courtCaseEntity = mock(CourtCaseEntity.class);
        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setCourtCase(courtCaseEntity);

        HearingsResponseHearing hearingsResponseHearing = mock(HearingsResponseHearing.class);
        HearingsResponseCase hearingsResponseCase = mock(HearingsResponseCase.class);


        adminHearingMapperMockedStatic.when(() -> AdminHearingMapper.mapToHearingsResponseHearing(hearingEntity))
            .thenReturn(hearingsResponseHearing);
        adminHearingMapperMockedStatic.when(() -> AdminHearingMapper.mapToHearingsResponseCase(courtCaseEntity))
            .thenReturn(hearingsResponseCase);


        HearingsResponse hearingsResponse = AdminHearingMapper.mapToHearingsResponse(hearingEntity);
        assertThat(hearingsResponse).isNotNull();
        assertThat(hearingsResponse.getHearing()).isEqualTo(hearingsResponseHearing);
        assertThat(hearingsResponse.getCase()).isEqualTo(hearingsResponseCase);
    }

    @Test
    void mapToHearingsResponseHearing_hasNullInput_shouldReturnNull() {
        assertThat(AdminHearingMapper.mapToHearingsResponseHearing(null)).isNull();
    }

    @Test
    void mapToHearingsResponseHearing_hasData_shouldReturnMappedValues() {
        fail("TODO");
    }

    @Test
    void mapToHearingsResponseCase_hasNullInput_shouldReturnNull() {
        assertThat(AdminHearingMapper.mapToHearingsResponseCase(null)).isNull();
    }

    @Test
    void mapToHearingsResponseCase_hasData_shouldReturnMappedValues() {
        fail("TODO");
    }
}
