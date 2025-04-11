package uk.gov.hmcts.darts.hearings.mapper;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.DefenceEntity;
import uk.gov.hmcts.darts.common.entity.DefendantEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.JudgeEntity;
import uk.gov.hmcts.darts.common.entity.ProsecutorEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.hearings.model.HearingsResponse;
import uk.gov.hmcts.darts.hearings.model.HearingsResponseCase;
import uk.gov.hmcts.darts.hearings.model.HearingsResponseCaseCourthouse;
import uk.gov.hmcts.darts.hearings.model.HearingsResponseCourtroom;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminHearingMapperTest {
    MockedStatic<AdminHearingMapper> adminHearingMapperMockedStatic;

    @BeforeEach
    void beforeEach() {
        adminHearingMapperMockedStatic = Mockito.mockStatic(AdminHearingMapper.class, Mockito.CALLS_REAL_METHODS);
    }

    @AfterEach
    void afterEach() {
        adminHearingMapperMockedStatic.close();
    }


    @Test
    void mapToHearingsResponse_hasNullInput_shouldReturnNull() {
        assertThat(AdminHearingMapper.mapToHearingsResponse(null)).isNull();
    }

    @Test
    void mapToHearingsResponse_hasData_shouldReturnMappedResposne() {
        final LocalDate hearingDate = LocalDate.of(2000, 1, 1);
        final OffsetDateTime cratedDate = OffsetDateTime.now().minusDays(2);
        final OffsetDateTime updatedDate = OffsetDateTime.now().minusDays(2);
        final UserAccountEntity createdBy = mock(UserAccountEntity.class);
        final UserAccountEntity updatedBy = mock(UserAccountEntity.class);
        when(createdBy.getId()).thenReturn(3);
        when(updatedBy.getId()).thenReturn(4);


        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setId(123);
        hearingEntity.setHearingDate(hearingDate);
        hearingEntity.setHearingIsActual(true);
        CourtCaseEntity courtCaseEntity = mock(CourtCaseEntity.class);
        hearingEntity.setCourtCase(courtCaseEntity);
        CourtroomEntity courtroomEntity = mock(CourtroomEntity.class);
        hearingEntity.setCourtroom(courtroomEntity);
        hearingEntity.setJudges(Set.of(
            craeteJudges("judges1"),
            craeteJudges("judges2"),
            craeteJudges("judges3")
        ));
        hearingEntity.setCreatedDateTime(cratedDate);
        hearingEntity.setCreatedBy(createdBy);
        hearingEntity.setLastModifiedDateTime(updatedDate);
        hearingEntity.setLastModifiedBy(updatedBy);

        HearingsResponseCase hearingsResponseCase = mock(HearingsResponseCase.class);
        adminHearingMapperMockedStatic.when(() -> AdminHearingMapper.mapToHearingsResponseCase(courtCaseEntity)).thenReturn(hearingsResponseCase);
        HearingsResponseCourtroom hearingsResponseCourtroom = mock(HearingsResponseCourtroom.class);
        adminHearingMapperMockedStatic.when(() -> AdminHearingMapper.mapToCourtroom(courtroomEntity)).thenReturn(hearingsResponseCourtroom);

        HearingsResponse hearingsResponse = AdminHearingMapper.mapToHearingsResponse(hearingEntity);
        assertThat(hearingsResponse).isNotNull();
        assertThat(hearingsResponse.getId()).isEqualTo(123);
        assertThat(hearingsResponse.getHearingDate()).isEqualTo(hearingDate);
        assertThat(hearingsResponse.getHearingIsActual()).isTrue();
        assertThat(hearingsResponse.getCase()).isEqualTo(hearingsResponseCase);
        assertThat(hearingsResponse.getCourtroom()).isEqualTo(hearingsResponseCourtroom);
        assertThat(hearingsResponse.getJudges()).hasSize(3).containsExactlyInAnyOrder("judges1", "judges2", "judges3");
        assertThat(hearingsResponse.getCreatedAt()).isEqualTo(cratedDate);
        assertThat(hearingsResponse.getCreatedBy()).isEqualTo(3);
        assertThat(hearingsResponse.getLastModifiedAt()).isEqualTo(updatedDate);
        assertThat(hearingsResponse.getLastModifiedBy()).isEqualTo(4);
    }


    @Test
    void mapToHearingsResponseCase_hasNullInput_shouldReturnNull() {
        assertThat(AdminHearingMapper.mapToHearingsResponseCase(null)).isNull();
    }

    @Test
    void mapToHearingsResponseCase_hasData_shouldReturnMappedResposne() {
        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(123);
        courtCaseEntity.setCaseNumber("case-number");
        CourthouseEntity courthouseEntity = mock(CourthouseEntity.class);
        courtCaseEntity.setCourthouse(courthouseEntity);
        courtCaseEntity.setDefendantList(List.of(
            craeteDefendants("defendant1"),
            craeteDefendants("defendant2"),
            craeteDefendants("defendant3")
        ));
        courtCaseEntity.setProsecutorList(List.of(
            craeteProsecutors("prosecutors1"),
            craeteProsecutors("prosecutors2"),
            craeteProsecutors("prosecutors3")
        ));
        courtCaseEntity.setDefenceList(List.of(
            craeteDefenders("efenders1"),
            craeteDefenders("efenders2"),
            craeteDefenders("efenders3")
        ));
        courtCaseEntity.setJudges(Set.of(
            craeteJudges("judges1"),
            craeteJudges("judges2"),
            craeteJudges("judges3")
        ));

        HearingsResponseCaseCourthouse courthouse = mock(HearingsResponseCaseCourthouse.class);
        adminHearingMapperMockedStatic.when(() -> AdminHearingMapper.mapToCourtHouse(courthouseEntity)).thenReturn(courthouse);

        HearingsResponseCase hearingsResponseCase = AdminHearingMapper.mapToHearingsResponseCase(courtCaseEntity);
        assertThat(hearingsResponseCase).isNotNull();
        assertThat(hearingsResponseCase.getId()).isEqualTo(123);
        assertThat(hearingsResponseCase.getCaseNumber()).isEqualTo("case-number");
        assertThat(hearingsResponseCase.getCourthouse()).isEqualTo(courthouse);
        assertThat(hearingsResponseCase.getDefendants()).hasSize(3).containsExactly("defendant1", "defendant2", "defendant3");
        assertThat(hearingsResponseCase.getProsecutors()).hasSize(3).containsExactly("prosecutors1", "prosecutors2", "prosecutors3");
        assertThat(hearingsResponseCase.getDefenders()).hasSize(3).containsExactly("efenders1", "efenders2", "efenders3");
        assertThat(hearingsResponseCase.getJudges()).hasSize(3).containsExactlyInAnyOrder("judges1", "judges2", "judges3");
    }

    @Test
    void mapToCourtHouse_hasNullInput_shouldReturnNull() {
        assertThat(AdminHearingMapper.mapToCourtHouse(null)).isNull();
    }

    @Test
    void mapToCourtHouse_hasData_shouldReturnMappedResposne() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(123);
        courthouseEntity.setDisplayName("some-name");

        HearingsResponseCaseCourthouse courthouse = AdminHearingMapper.mapToCourtHouse(courthouseEntity);
        assertThat(courthouse).isNotNull();
        assertThat(courthouse.getId()).isEqualTo(123);
        assertThat(courthouse.getDisplayName()).isEqualTo("some-name");
    }

    @Test
    void mapToCourtroom_hasNullInput_shouldReturnNull() {
        assertThat(AdminHearingMapper.mapToCourtroom(null)).isNull();
    }

    @Test
    void mapToCourtroom_hasData_shouldReturnMappedResposne() {
        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setId(123);
        courtroomEntity.setName("some-name");

        HearingsResponseCourtroom courtroom = AdminHearingMapper.mapToCourtroom(courtroomEntity);
        assertThat(courtroom).isNotNull();
        assertThat(courtroom.getId()).isEqualTo(123);
        assertThat(courtroom.getName()).isEqualTo("SOME-NAME");
    }


    @Test
    void asList_hasNullInput_shouldReturnEmptyList() {
        assertThat(AdminHearingMapper.asList(null, object -> object))
            .isNotNull()
            .isEmpty();
    }

    @Test
    void asList_hasNullData_shouldReturnListExcludingNullValues() {
        List<String> list = new ArrayList<>();
        list.add("value");
        list.add(null);
        list.add("value2");

        List<String> result = AdminHearingMapper.asList(list, object -> {
            assertThat(object).isNotNull();
            return object;
        });
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2).containsExactly("value", "value2");
    }

    @Test
    void asList_hasData_shouldReturnListWithMappedValues() {
        List<String> list = new ArrayList<>();
        list.add("value");
        list.add("value2");
        list.add("value3");

        List<String> result = AdminHearingMapper.asList(list, object -> {
            assertThat(object).isNotNull();
            return object;
        });
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3).containsExactly("value", "value2", "value3");
    }

    @Test
    void toTimeStringOffsetDateTime_hasNullInput_shouldReturnNull() {
        assertThat(AdminHearingMapper.toTimeString((OffsetDateTime) null)).isNull();
    }

    @Test
    void toTimeStringOffsetDateTime_hasData_shouldReturnMappedValues() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDate.now(), LocalTime.of(12, 23, 12),
                                                          OffsetDateTime.now().getOffset());
        assertThat(AdminHearingMapper.toTimeString(offsetDateTime)).isEqualTo("12:23:12");
    }

    @Test
    void toTimeStringOffsetDateTime_hasDataWith0Values_shouldReturnMappedValues() {
        OffsetDateTime offsetDateTime = OffsetDateTime.of(LocalDate.now(), LocalTime.of(0, 0, 0),
                                                          OffsetDateTime.now().getOffset());
        assertThat(AdminHearingMapper.toTimeString(offsetDateTime)).isEqualTo("00:00:00");
    }

    @Test
    void toTimeStringLocalTime_hasNullInput_shouldReturnNull() {
        assertThat(AdminHearingMapper.toTimeString((LocalTime) null)).isNull();
    }

    @Test
    void toTimeStringLocalTime_hasData_shouldReturnMappedValues() {
        LocalTime localTime = LocalTime.of(12, 23, 12);
        assertThat(AdminHearingMapper.toTimeString(localTime)).isEqualTo("12:23:12");
    }

    @Test
    void toTimeStringLocalTime_hasDataWith0Values_shouldReturnMappedValues() {
        LocalTime localTime = LocalTime.of(0, 0, 0);
        assertThat(AdminHearingMapper.toTimeString(localTime)).isEqualTo("00:00:00");
    }

    private JudgeEntity craeteJudges(String name) {
        JudgeEntity entity = new JudgeEntity();
        entity.setName(name);
        return entity;
    }

    private DefenceEntity craeteDefenders(String name) {
        DefenceEntity entity = new DefenceEntity();
        entity.setName(name);
        return entity;
    }

    private ProsecutorEntity craeteProsecutors(String name) {
        ProsecutorEntity entity = new ProsecutorEntity();
        entity.setName(name);
        return entity;
    }

    private DefendantEntity craeteDefendants(String name) {
        DefendantEntity entity = new DefendantEntity();
        entity.setName(name);
        return entity;
    }
}
