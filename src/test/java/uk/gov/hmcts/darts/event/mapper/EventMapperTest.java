package uk.gov.hmcts.darts.event.mapper;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.event.model.AdminGetEventById200Response;
import uk.gov.hmcts.darts.event.model.AdminGetEventResponseDetails;
import uk.gov.hmcts.darts.event.model.AdminGetEventResponseDetailsCasesCasesInner;
import uk.gov.hmcts.darts.event.model.AdminGetEventResponseDetailsHearingsHearingsInner;
import uk.gov.hmcts.darts.event.model.AdminGetVersionsByEventIdResponseResult;
import uk.gov.hmcts.darts.event.model.CourthouseResponseDetails;
import uk.gov.hmcts.darts.event.model.CourtroomResponseDetails;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventMapperTest {

    private EventMapper eventMapper;

    @BeforeEach
    public void before() {
        eventMapper = spy(new EventMapper());
    }

    @Test
    void mapsEventToAdminSearchEventResponseResult() {
        // When
        EventEntity eventEntity = spy(setGenericEventDataForTest());
        eventEntity.setId(2);
        eventEntity.setCreatedDateTime(OffsetDateTime.now());
        eventEntity.setIsCurrent(true);


        CourtCaseEntity courtCaseEntity1 = mock(CourtCaseEntity.class);
        CourtCaseEntity courtCaseEntity2 = mock(CourtCaseEntity.class);
        CourtCaseEntity courtCaseEntity3 = mock(CourtCaseEntity.class);
        doReturn(List.of(courtCaseEntity1, courtCaseEntity2, courtCaseEntity3)).when(eventEntity).getLinkedCases();

        AdminGetEventResponseDetailsCasesCasesInner adminGetEventResponseDetailsCasesCasesInner1 = mock(AdminGetEventResponseDetailsCasesCasesInner.class);
        AdminGetEventResponseDetailsCasesCasesInner adminGetEventResponseDetailsCasesCasesInner2 = mock(AdminGetEventResponseDetailsCasesCasesInner.class);
        AdminGetEventResponseDetailsCasesCasesInner adminGetEventResponseDetailsCasesCasesInner3 = mock(AdminGetEventResponseDetailsCasesCasesInner.class);

        doReturn(adminGetEventResponseDetailsCasesCasesInner1).when(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity1);
        doReturn(adminGetEventResponseDetailsCasesCasesInner2).when(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity2);
        doReturn(adminGetEventResponseDetailsCasesCasesInner3).when(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity3);

        HearingEntity hearingEntity1 = mock(HearingEntity.class);
        HearingEntity hearingEntity2 = mock(HearingEntity.class);
        HearingEntity hearingEntity3 = mock(HearingEntity.class);
        doReturn(List.of(hearingEntity1, hearingEntity2, hearingEntity3)).when(eventEntity).getHearingEntities();

        AdminGetEventResponseDetailsHearingsHearingsInner adminGetEventResponseDetailsHearingsHearingsInner1 = mock(
            AdminGetEventResponseDetailsHearingsHearingsInner.class);
        AdminGetEventResponseDetailsHearingsHearingsInner adminGetEventResponseDetailsHearingsHearingsInner2 = mock(
            AdminGetEventResponseDetailsHearingsHearingsInner.class);
        AdminGetEventResponseDetailsHearingsHearingsInner adminGetEventResponseDetailsHearingsHearingsInner3 = mock(
            AdminGetEventResponseDetailsHearingsHearingsInner.class);

        doReturn(adminGetEventResponseDetailsHearingsHearingsInner1).when(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity1);
        doReturn(adminGetEventResponseDetailsHearingsHearingsInner2).when(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity2);
        doReturn(adminGetEventResponseDetailsHearingsHearingsInner3).when(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity3);


        // Given
        AdminGetEventById200Response responseResult = eventMapper.mapToAdminGetEventById200Response(eventEntity);

        // Then
        assertAdminEventResponseDetails(eventEntity, responseResult);

        assertThat(responseResult.getCases())
            .containsExactly(adminGetEventResponseDetailsCasesCasesInner1, adminGetEventResponseDetailsCasesCasesInner2,
                             adminGetEventResponseDetailsCasesCasesInner3);

        assertThat(responseResult.getHearings())
            .containsExactly(adminGetEventResponseDetailsHearingsHearingsInner1, adminGetEventResponseDetailsHearingsHearingsInner2,
                             adminGetEventResponseDetailsHearingsHearingsInner3);

        verify(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity1);
        verify(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity2);
        verify(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity3);
        verify(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity1);
        verify(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity2);
        verify(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity3);
    }

    @Test
    void whenSingleVersionForAnEvent_mapsEventVersionToAdminGetEventVersionsResponseResult() {
        // When
        OffsetDateTime now = OffsetDateTime.now();
        EventEntity eventEntity1 = setGenericEventDataForTest();
        eventEntity1.setId(1);
        eventEntity1.setCreatedDateTime(now.minusDays(1));
        eventEntity1.setIsCurrent(true);

        List<EventEntity> eventEntities = List.of(eventEntity1);

        // Given
        AdminGetVersionsByEventIdResponseResult responseResult = eventMapper.mapToAdminGetEventVersionsResponseForId(eventEntities);

        AdminGetEventResponseDetails currentVersion = responseResult.getCurrentVersion();
        // Then
        assertAdminEventResponseDetails(eventEntity1, currentVersion);

        assertThat(responseResult.getPreviousVersions()).hasSize(0);
    }

    @Test
    @SuppressWarnings({"PMD.NcssCount"})
    void whenMultipleVersionsForAnEvent_mapsEventVersionToAdminGetEventVersionsResponseResult() {
        // When
        OffsetDateTime now = OffsetDateTime.now();
        EventEntity eventEntity1 = setGenericEventDataForTest();
        eventEntity1.setId(1);
        eventEntity1.setCreatedDateTime(now.minusDays(1));
        eventEntity1.setIsCurrent(true);

        EventEntity eventEntity2 = setGenericEventDataForTest();
        eventEntity2.setId(2);
        eventEntity2.setCreatedDateTime(now);
        eventEntity2.setIsCurrent(true);

        EventEntity eventEntity3 = setGenericEventDataForTest();
        eventEntity3.setId(3);
        eventEntity3.setCreatedDateTime(now.minusDays(2));
        eventEntity3.setIsCurrent(true);

        EventEntity eventEntity4 = setGenericEventDataForTest();
        eventEntity4.setId(4);
        eventEntity4.setCreatedDateTime(now);
        eventEntity4.setIsCurrent(false);

        List<EventEntity> eventEntities = List.of(eventEntity1, eventEntity2, eventEntity3, eventEntity4);

        // Given
        AdminGetVersionsByEventIdResponseResult responseResult = eventMapper.mapToAdminGetEventVersionsResponseForId(eventEntities);

        AdminGetEventResponseDetails currentVersion = responseResult.getCurrentVersion();
        // Then
        assertAdminEventResponseDetails(eventEntity2, currentVersion);

        List<AdminGetEventResponseDetails> previousVersions = responseResult.getPreviousVersions();
        assertEquals(3, previousVersions.size());

        assertAdminEventResponseDetails(eventEntity4, previousVersions.getFirst());

        assertAdminEventResponseDetails(eventEntity1, previousVersions.get(1));

        assertAdminEventResponseDetails(eventEntity3, previousVersions.get(2));
    }

    private static void assertAdminEventResponseDetails(EventEntity eventEntity2, AdminGetEventResponseDetails currentVersion) {
        assertEquals(eventEntity2.getId(), currentVersion.getId());
        assertEquals(eventEntity2.getLegacyObjectId(), currentVersion.getDocumentumId());
        assertEquals(eventEntity2.getEventId(), currentVersion.getSourceId());
        assertEquals(eventEntity2.getMessageId(), currentVersion.getMessageId());
        assertEquals(eventEntity2.getEventText(), currentVersion.getText());
        assertEquals(eventEntity2.getEventType().getId(), currentVersion.getEventMapping().getId());
        assertEquals(eventEntity2.isLogEntry(), currentVersion.getIsLogEntry());
        assertEquals(eventEntity2.getCourtroom().getId(), currentVersion.getCourtroom().getId());
        assertEquals(eventEntity2.getCourtroom().getName(), currentVersion.getCourtroom().getName());
        assertEquals(eventEntity2.getCourtroom().getCourthouse().getId(), currentVersion.getCourthouse().getId());
        assertEquals(eventEntity2.getCourtroom().getCourthouse().getDisplayName(), currentVersion.getCourthouse().getDisplayName());
        assertEquals(eventEntity2.getLegacyVersionLabel(), currentVersion.getVersion());
        assertEquals(eventEntity2.getChronicleId(), currentVersion.getChronicleId());
        assertEquals(eventEntity2.getAntecedentId(), currentVersion.getAntecedentId());
        assertEquals(eventEntity2.getTimestamp(), currentVersion.getEventTs());
        assertEquals(eventEntity2.getIsCurrent(), currentVersion.getIsCurrent());
        assertEquals(eventEntity2.getCreatedDateTime(), currentVersion.getCreatedAt());
        assertEquals(eventEntity2.getCreatedBy().getId(), currentVersion.getCreatedBy());
        assertEquals(eventEntity2.getLastModifiedDateTime(), currentVersion.getLastModifiedAt());
        assertEquals(eventEntity2.getLastModifiedById(), currentVersion.getLastModifiedBy());
        assertEquals(eventEntity2.isDataAnonymised(), currentVersion.getIsDataAnonymised());
    }

    @Test
    void whenSingleVersionForAnEventIsNotCurrent_mapsEventVersionToAdminGetEventVersionsResponseResultAndVersionSetInPreviousEvent() {
        // When
        OffsetDateTime now = OffsetDateTime.now();
        EventEntity eventEntity1 = setGenericEventDataForTest();
        eventEntity1.setId(1);
        eventEntity1.setCreatedDateTime(now.minusDays(1));
        eventEntity1.setIsCurrent(false);

        List<EventEntity> eventEntities = List.of(eventEntity1);
        // Given
        AdminGetVersionsByEventIdResponseResult responseResult = eventMapper.mapToAdminGetEventVersionsResponseForId(eventEntities);

        AdminGetEventResponseDetails previousVersion = responseResult.getPreviousVersions().getFirst();
        // Then
        assertNull(responseResult.getCurrentVersion());
        assertAdminEventResponseDetails(eventEntity1, previousVersion);
    }

    private EventEntity setGenericEventDataForTest() {
        EventEntity eventEntity = new EventEntity();

        eventEntity.setEventId(200);
        eventEntity.setLegacyObjectId("legacyObjectId");
        eventEntity.setMessageId("messageId");
        eventEntity.setEventText("eventText");

        EventHandlerEntity eventHandlerEntity = new EventHandlerEntity();
        eventHandlerEntity.setId(300);
        eventEntity.setEventType(eventHandlerEntity);
        eventEntity.setLogEntry(false);

        CourtroomEntity courtroom = new CourtroomEntity();
        courtroom.setId(500);
        courtroom.setName("courtroomName");

        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(700);
        courthouseEntity.setDisplayName("courthouseDisplayName");
        courtroom.setCourthouse(courthouseEntity);

        eventEntity.setCourtroom(courtroom);
        eventEntity.setLegacyVersionLabel("legacyVersionLabel");
        eventEntity.setChronicleId("chronicleId");
        eventEntity.setAntecedentId("antecedentId");

        UserAccountEntity uaCreatedEntity = new UserAccountEntity();
        eventEntity.setCreatedBy(uaCreatedEntity);

        UserAccountEntity uaLastModifiedEntity = new UserAccountEntity();
        eventEntity.setLastModifiedDateTime(OffsetDateTime.now());
        eventEntity.setLastModifiedBy(uaLastModifiedEntity);
        eventEntity.setTimestamp(OffsetDateTime.now());

        return eventEntity;
    }


    @Test
    void mapAdminGetEventResponseDetailsHearings_whenHearingEntityIsNull_returnsEmptyList() {
        assertThat(eventMapper.mapAdminGetEventResponseDetailsHearings(null)).isEmpty();
    }

    @Test
    void mapAdminGetEventResponseDetailsHearings_whenHearingEntityIsEmpty_returnsEmptyList() {
        assertThat(eventMapper.mapAdminGetEventResponseDetailsHearings(List.of())).isEmpty();
    }

    @Test
    void mapAdminGetEventResponseDetailsHearingsHearings_whenHasListValue_shouldMapAllValues() {
        HearingEntity hearingEntity1 = mock(HearingEntity.class);
        HearingEntity hearingEntity2 = mock(HearingEntity.class);
        HearingEntity hearingEntity3 = mock(HearingEntity.class);

        AdminGetEventResponseDetailsHearingsHearingsInner adminGetEventResponseDetailsHearingsHearingsInner1 = mock(
            AdminGetEventResponseDetailsHearingsHearingsInner.class);
        AdminGetEventResponseDetailsHearingsHearingsInner adminGetEventResponseDetailsHearingsHearingsInner2 = mock(
            AdminGetEventResponseDetailsHearingsHearingsInner.class);
        AdminGetEventResponseDetailsHearingsHearingsInner adminGetEventResponseDetailsHearingsHearingsInner3 = mock(
            AdminGetEventResponseDetailsHearingsHearingsInner.class);

        doReturn(adminGetEventResponseDetailsHearingsHearingsInner1).when(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity1);
        doReturn(adminGetEventResponseDetailsHearingsHearingsInner2).when(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity2);
        doReturn(adminGetEventResponseDetailsHearingsHearingsInner3).when(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity3);

        assertThat(eventMapper.mapAdminGetEventResponseDetailsHearings(List.of(hearingEntity1, hearingEntity2, hearingEntity3)))
            .containsExactly(adminGetEventResponseDetailsHearingsHearingsInner1, adminGetEventResponseDetailsHearingsHearingsInner2,
                             adminGetEventResponseDetailsHearingsHearingsInner3);

        verify(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity1);
        verify(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity2);
        verify(eventMapper).mapAdminGetEventResponseDetailsHearing(hearingEntity3);
    }

    @Test
    void mapAdminGetEventResponseDetailsHearing_whenNullValueIsGiven_returnNull() {
        assertThat(eventMapper.mapAdminGetEventResponseDetailsHearing(null))
            .isNull();
    }

    @Test
    void mapAdminGetEventResponseDetailsHearingsHearing_whenValueIsGiven_shouldMapAllValues() {
        CourthouseResponseDetails courthouseResponseDetails = mock(CourthouseResponseDetails.class);
        CourthouseEntity courthouseEntity = mock(CourthouseEntity.class);
        doReturn(courthouseResponseDetails).when(eventMapper).mapCourtHouse(courthouseEntity);

        CourtroomResponseDetails courtroomResponseDetails = mock(CourtroomResponseDetails.class);
        CourtroomEntity courtroomEntity = mock(CourtroomEntity.class);
        doReturn(courtroomResponseDetails).when(eventMapper).mapCourtRoom(courtroomEntity);
        when(courtroomEntity.getCourthouse()).thenReturn(courthouseEntity);

        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(123);
        courtCaseEntity.setCaseNumber("caseNumber");

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setId(1);
        hearingEntity.setCourtCase(courtCaseEntity);
        LocalDate hearingDate = LocalDate.now();
        hearingEntity.setHearingDate(hearingDate);
        hearingEntity.setCourtroom(courtroomEntity);

        AdminGetEventResponseDetailsHearingsHearingsInner result = eventMapper.mapAdminGetEventResponseDetailsHearing(hearingEntity);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getCaseId()).isEqualTo(123);
        assertThat(result.getCaseNumber()).isEqualTo("caseNumber");
        assertThat(result.getHearingDate()).isEqualTo(hearingDate);
        assertThat(result.getCourtroom()).isEqualTo(courtroomResponseDetails);
        assertThat(result.getCourthouse()).isEqualTo(courthouseResponseDetails);

        verify(eventMapper).mapCourtHouse(courthouseEntity);
        verify(eventMapper).mapCourtRoom(courtroomEntity);
    }

    @Test
    void mapAdminGetEventResponseDetailsHearingsHearing_whenMissingCourtRoom_shouldMapAllFieldsBarCourtRoomAndCourtHouse() {
        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(123);
        courtCaseEntity.setCaseNumber("caseNumber");

        HearingEntity hearingEntity = new HearingEntity();
        hearingEntity.setId(1);
        hearingEntity.setCourtCase(courtCaseEntity);
        LocalDate hearingDate = LocalDate.now();
        hearingEntity.setHearingDate(hearingDate);

        AdminGetEventResponseDetailsHearingsHearingsInner result = eventMapper.mapAdminGetEventResponseDetailsHearing(hearingEntity);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getCaseId()).isEqualTo(123);
        assertThat(result.getCaseNumber()).isEqualTo("caseNumber");
        assertThat(result.getHearingDate()).isEqualTo(hearingDate);
        assertThat(result.getCourtroom()).isNull();
        assertThat(result.getCourthouse()).isNull();
    }

    @Test
    void mapAdminGetEventResponseDetailsCasesCases_whenHearingEntityIsNull_returnsEmptyList() {
        assertThat(eventMapper.mapAdminGetEventResponseDetailsCasesCases(null)).isEmpty();
    }

    @Test
    void mapAdminGetEventResponseDetailsCasesCases_whenHearingEntityIsEmpty_returnsEmptyList() {
        assertThat(eventMapper.mapAdminGetEventResponseDetailsCasesCases(List.of())).isEmpty();
    }

    @Test
    void mapAdminGetEventResponseDetailsCasesCases_whenHasListValue_shouldMapAllValues() {
        CourtCaseEntity courtCaseEntity1 = mock(CourtCaseEntity.class);
        CourtCaseEntity courtCaseEntity2 = mock(CourtCaseEntity.class);
        CourtCaseEntity courtCaseEntity3 = mock(CourtCaseEntity.class);

        AdminGetEventResponseDetailsCasesCasesInner adminGetEventResponseDetailsCasesCasesInner1 = mock(AdminGetEventResponseDetailsCasesCasesInner.class);
        AdminGetEventResponseDetailsCasesCasesInner adminGetEventResponseDetailsCasesCasesInner2 = mock(AdminGetEventResponseDetailsCasesCasesInner.class);
        AdminGetEventResponseDetailsCasesCasesInner adminGetEventResponseDetailsCasesCasesInner3 = mock(AdminGetEventResponseDetailsCasesCasesInner.class);

        doReturn(adminGetEventResponseDetailsCasesCasesInner1).when(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity1);
        doReturn(adminGetEventResponseDetailsCasesCasesInner2).when(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity2);
        doReturn(adminGetEventResponseDetailsCasesCasesInner3).when(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity3);

        assertThat(eventMapper.mapAdminGetEventResponseDetailsCasesCases(List.of(courtCaseEntity1, courtCaseEntity2, courtCaseEntity3)))
            .containsExactly(adminGetEventResponseDetailsCasesCasesInner1, adminGetEventResponseDetailsCasesCasesInner2,
                             adminGetEventResponseDetailsCasesCasesInner3);

        verify(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity1);
        verify(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity2);
        verify(eventMapper).mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity3);
    }

    @Test
    void mapAdminGetEventResponseDetailsCasesCase_whenNullValueIsGiven_returnNull() {
        assertThat(eventMapper.mapAdminGetEventResponseDetailsCasesCase(null))
            .isNull();
    }

    @Test
    void mapAdminGetEventResponseDetailsCasesCase_whenValueIsGiven_shouldMapAllValues() {
        CourthouseResponseDetails courthouseResponseDetails = mock(CourthouseResponseDetails.class);
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        doReturn(courthouseResponseDetails).when(eventMapper).mapCourtHouse(courthouseEntity);

        CourtCaseEntity courtCaseEntity = new CourtCaseEntity();
        courtCaseEntity.setId(1);
        courtCaseEntity.setCaseNumber("caseNumber");
        courtCaseEntity.setCourthouse(courthouseEntity);
        AdminGetEventResponseDetailsCasesCasesInner result = eventMapper.mapAdminGetEventResponseDetailsCasesCase(courtCaseEntity);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getCaseNumber()).isEqualTo("caseNumber");
        assertThat(result.getCourthouse()).isEqualTo(courthouseResponseDetails);
        verify(eventMapper).mapCourtHouse(courthouseEntity);
    }

    @Test
    void mapCourtHouse_whenNullValueIsGiven_returnNull() {
        assertThat(eventMapper.mapCourtHouse(null))
            .isNull();
    }

    @Test
    void mapCourtHouse_whenValueIsGiven_shouldMapAllValues() {
        CourthouseEntity courthouseEntity = new CourthouseEntity();
        courthouseEntity.setId(1);
        courthouseEntity.setDisplayName("name");

        CourthouseResponseDetails courthouseResponseDetails = eventMapper.mapCourtHouse(courthouseEntity);
        assertThat(courthouseResponseDetails).isNotNull();
        assertThat(courthouseResponseDetails.getId()).isEqualTo(1);
        assertThat(courthouseResponseDetails.getDisplayName()).isEqualTo("name");
    }

    @Test
    void mapCourtRoom_whenNullValueIsGiven_returnNull() {
        assertThat(eventMapper.mapCourtRoom(null))
            .isNull();
    }

    @Test
    void mapCourtRoom_whenValueIsGiven_shouldMapAllValues() {
        CourtroomEntity courtroomEntity = new CourtroomEntity();
        courtroomEntity.setId(1);
        courtroomEntity.setName("name");

        CourtroomResponseDetails courtroomResponseDetails = eventMapper.mapCourtRoom(courtroomEntity);
        assertThat(courtroomResponseDetails).isNotNull();
        assertThat(courtroomResponseDetails.getId()).isEqualTo(1);
        assertThat(courtroomResponseDetails.getName()).isEqualTo("NAME");
    }
}