package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadResponseMetaData;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.ExternalLocationTypeEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.ExternalLocationTypeEnum;
import uk.gov.hmcts.darts.common.enums.SystemUsersEnum;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;
import uk.gov.hmcts.darts.common.helper.SystemUserHelper;
import uk.gov.hmcts.darts.common.repository.CourthouseRepository;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.service.CreateCoreObjectService;
import uk.gov.hmcts.darts.common.service.RetrieveCoreObjectService;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.dailylist.mapper.CitizenNameMapper;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.util.CitizenNameComparator;
import uk.gov.hmcts.darts.dets.service.DetsApiService;
import uk.gov.hmcts.darts.task.runner.dailylist.mapper.DailyListRequestMapper;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.DailyListStructure;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.XmlParser;
import uk.gov.hmcts.darts.test.common.TestUtils;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.FAILED;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.PARTIALLY_PROCESSED;
import static uk.gov.hmcts.darts.dailylist.enums.JobStatusType.PROCESSED;

@ExtendWith(MockitoExtension.class)
class DailyListUpdaterTest {

    @Mock
    private RetrieveCoreObjectService retrieveCoreObjectService;
    @Mock
    private CreateCoreObjectService createCoreObjectService;
    @Mock
    private CourthouseRepository courthouseRepository;
    @Mock
    private HearingRepository hearingRepository;
    @Mock
    private SystemUserHelper systemUserHelper;
    @Mock
    private CurrentTimeHelper currentTimeHelper;
    @Mock
    private CitizenNameMapper citizenNameMapper;
    @Mock
    private CitizenNameComparator citizenNameComparator;

    @Mock
    private DetsApiService detsApiService;
    @Mock
    private XmlParser xmlParser;
    @Mock
    private DailyListRequestMapper dailyListRequestMapper;

    private DailyListUpdater dailyListUpdater;

    @Captor
    private ArgumentCaptor<HearingEntity> hearingEntityCaptor;

    private static final LocalDateTime HEARING_DATE = LocalDateTime.of(2023, 9, 23, 11, 0, 0);

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = JsonMapper.builder().addModule(new JavaTimeModule()).build();
        dailyListUpdater = spy(new DailyListUpdater(retrieveCoreObjectService, createCoreObjectService, courthouseRepository,
                                                    hearingRepository, objectMapper, systemUserHelper,
                                                    currentTimeHelper, citizenNameMapper, citizenNameComparator,
                                                    detsApiService, xmlParser, dailyListRequestMapper));
    }

    @Test
    void processDailyListValidateJsonFailed() throws Exception {
        var dailyListUser = new UserAccountEntity();
        when(systemUserHelper.getDailyListProcessorUser()).thenReturn(dailyListUser);
        DailyListEntity dailyList = setUpDailyList("dailyList.json");
        doReturn(false).when(dailyListUpdater).validateJsonExistsElseUpdate(dailyList);

        dailyListUpdater.processDailyList(dailyList);
        assertThat(dailyList.getStatus()).isEqualTo(FAILED);
        verify(dailyListUpdater, times(1)).validateJsonExistsElseUpdate(dailyList);
    }

    @Test
    void handlesCaseNumberMissingForCpp() throws IOException {
        var dailyListUser = new UserAccountEntity();
        when(systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR)).thenReturn(dailyListUser);
        when(courthouseRepository.findByCourthouseName("SWANSEA")).thenReturn(Optional.of(new CourthouseEntity()));
        HearingEntity hearing = new HearingEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        hearing.setCourtCase(courtCase);

        DailyListEntity dailyList = setUpDailyList("dailyListWithoutUrn.json");

        doReturn(true).when(dailyListUpdater).validateJsonExistsElseUpdate(dailyList);
        dailyListUpdater.processDailyList(dailyList);

        verifyNoInteractions(retrieveCoreObjectService);

        assertThat(dailyList.getStatus()).isEqualTo(PARTIALLY_PROCESSED);
        verify(dailyListUpdater, times(1)).validateJsonExistsElseUpdate(dailyList);
    }

    @Test
    void handlesCaseNumberForCpp() throws IOException {
        var dailyListUser = new UserAccountEntity();
        OffsetDateTime testTime = DateConverterUtil.toOffsetDateTime(HEARING_DATE);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);
        when(systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR)).thenReturn(dailyListUser);
        when(courthouseRepository.findByCourthouseName("SWANSEA")).thenReturn(Optional.of(new CourthouseEntity()));
        HearingEntity hearing = new HearingEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        hearing.setCourtCase(courtCase);
        when(retrieveCoreObjectService.retrieveOrCreateHearing("SWANSEA", "1A", "42GD2391421", HEARING_DATE, dailyListUser))
            .thenReturn(hearing);
        DailyListEntity dailyList = setUpDailyList("dailyList.json");
        doReturn(true).when(dailyListUpdater).validateJsonExistsElseUpdate(dailyList);

        dailyListUpdater.processDailyList(dailyList);

        verify(hearingRepository, times(1)).saveAndFlush(hearingEntityCaptor.capture());

        HearingEntity hearingEntityCaptorValue = hearingEntityCaptor.getValue();
        assertThat(hearingEntityCaptorValue.getLastModifiedDateTime()).isEqualTo(testTime);
        assertThat(hearingEntityCaptorValue.getCourtCase().getLastModifiedDateTime()).isEqualTo(testTime);

        assertThat(dailyList.getStatus()).isEqualTo(PROCESSED);
        verify(dailyListUpdater, times(1)).validateJsonExistsElseUpdate(dailyList);
    }

    @Test
    void handlesNoTimeMarkingNote() throws IOException {

        var dailyListUser = new UserAccountEntity();
        OffsetDateTime testTime = DateConverterUtil.toOffsetDateTime(HEARING_DATE);
        when(currentTimeHelper.currentOffsetDateTime()).thenReturn(testTime);
        when(systemUserHelper.getReferenceTo(SystemUsersEnum.DAILY_LIST_PROCESSOR)).thenReturn(dailyListUser);
        when(courthouseRepository.findByCourthouseName("SWANSEA")).thenReturn(Optional.of(new CourthouseEntity()));
        HearingEntity hearing = new HearingEntity();
        CourtCaseEntity courtCase = new CourtCaseEntity();
        hearing.setCourtCase(courtCase);
        LocalDateTime expectedHearingDate = LocalDateTime.of(2023, 9, 23, 9, 0, 0);
        when(retrieveCoreObjectService.retrieveOrCreateHearing("SWANSEA", "1A", "42GD2391421", expectedHearingDate, dailyListUser))
            .thenReturn(hearing);
        DailyListEntity dailyList = setUpDailyList("handlesNoTimeMarkingNote.json");
        dailyListUpdater.processDailyList(dailyList);
        verify(retrieveCoreObjectService, times(1)).retrieveOrCreateHearing("SWANSEA", "1A", "42GD2391421", expectedHearingDate, dailyListUser);
    }

    @ParameterizedTest
    @CsvSource({
        "NOT BEFORE 10:00 am, 10:00",
        "NOT BEFORE  10:00 am, 10:00",
        "SITTING AT  10:00 am, 10:00",
        "SITTING AT 10:00 am, 10:00",
        "11:00 am, 11:00",
        "3:00 PM, 15:00"
    })
    void getTimeFromTimeMarkingNote(String timeMarkingNote, String result) {
        assertThat(dailyListUpdater.getTimeFromTimeMarkingNote(timeMarkingNote)).isEqualTo(result);
    }

    private DailyListEntity setUpDailyList(String filename) throws IOException {
        String dailyListJson = TestUtils.getContentsFromFile(
            "Tests/dailylist/DailyListUpdaterTest/" + filename);
        dailyListJson = dailyListJson.replace("todays_date", HEARING_DATE.toLocalDate().toString());

        DailyListEntity dailyList = new DailyListEntity();
        dailyList.setId(1);
        dailyList.setSource("CPP");
        dailyList.setContent(dailyListJson);
        return dailyList;
    }


    @Nested
    class ValidateJsonExistsElseUpdate {

        @Test
        void alreadyHasJson() {
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setContent("{\"has_json\": true}");
            assertThat(dailyListUpdater.validateJsonExistsElseUpdate(dailyListEntity))
                .isTrue();

            verify(dailyListUpdater, times(1)).validateJsonExistsElseUpdate(dailyListEntity);
            verifyNoMoreInteractions(dailyListUpdater);
        }

        @Test
        void missingJsonFailedXmlUpdate() {
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setContent(null);
            doReturn(false).when(dailyListUpdater).validateXmlElseUpdate(dailyListEntity);

            assertThat(dailyListUpdater.validateJsonExistsElseUpdate(dailyListEntity))
                .isFalse();

            verify(dailyListUpdater, times(1)).validateJsonExistsElseUpdate(dailyListEntity);
            verify(dailyListUpdater, times(1)).validateXmlElseUpdate(dailyListEntity);
            verifyNoMoreInteractions(dailyListUpdater);
        }

        @Test
        void missingJsonPassedXmlUpdateFailedJsonMapping() {
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setContent(null);
            doReturn(true).when(dailyListUpdater).validateXmlElseUpdate(dailyListEntity);
            doReturn(false).when(dailyListUpdater).mapXmlToJson(dailyListEntity);

            assertThat(dailyListUpdater.validateJsonExistsElseUpdate(dailyListEntity))
                .isFalse();

            verify(dailyListUpdater, times(1)).validateJsonExistsElseUpdate(dailyListEntity);
            verify(dailyListUpdater, times(1)).validateXmlElseUpdate(dailyListEntity);
            verify(dailyListUpdater, times(1)).mapXmlToJson(dailyListEntity);
            verifyNoMoreInteractions(dailyListUpdater);
        }

        @Test
        void missingJsonPassedXmlUpdatePassefdJsonMapping() {
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setContent(null);
            doReturn(true).when(dailyListUpdater).validateXmlElseUpdate(dailyListEntity);
            doReturn(true).when(dailyListUpdater).mapXmlToJson(dailyListEntity);

            assertThat(dailyListUpdater.validateJsonExistsElseUpdate(dailyListEntity))
                .isTrue();

            verify(dailyListUpdater, times(1)).validateJsonExistsElseUpdate(dailyListEntity);
            verify(dailyListUpdater, times(1)).validateXmlElseUpdate(dailyListEntity);
            verify(dailyListUpdater, times(1)).mapXmlToJson(dailyListEntity);
            verifyNoMoreInteractions(dailyListUpdater);
        }
    }


    @Nested
    class ValidateXmlElseUpdate {

        @Test
        void missingExternalLocation() {
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setExternalLocation(null);
            ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
            externalLocationTypeEntity.setId(4);
            dailyListEntity.setExternalLocationTypeEntity(externalLocationTypeEntity);

            assertThat(dailyListUpdater.validateXmlElseUpdate(dailyListEntity))
                .isFalse();

            verify(dailyListUpdater, times(1)).validateXmlElseUpdate(dailyListEntity);
            verifyNoInteractions(detsApiService);
            assertThat(dailyListEntity.getXmlContent()).isNull();
        }

        @Test
        void missingEltId() {
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setExternalLocation(UUID.randomUUID());
            dailyListEntity.setExternalLocationTypeEntity(null);

            assertThat(dailyListUpdater.validateXmlElseUpdate(dailyListEntity))
                .isFalse();

            verify(dailyListUpdater, times(1)).validateXmlElseUpdate(dailyListEntity);
            verifyNoInteractions(detsApiService);
            assertThat(dailyListEntity.getXmlContent()).isNull();
        }

        @ParameterizedTest
        @EnumSource(value = ExternalLocationTypeEnum.class, mode = EnumSource.Mode.EXCLUDE, names = {"DETS"})
        void incorrectEltId(ExternalLocationTypeEnum externalLocationTypeEnum) {
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setExternalLocation(UUID.randomUUID());
            ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
            externalLocationTypeEntity.setId(externalLocationTypeEnum.getId());
            dailyListEntity.setExternalLocationTypeEntity(externalLocationTypeEntity);

            assertThat(dailyListUpdater.validateXmlElseUpdate(dailyListEntity))
                .isFalse();

            verify(dailyListUpdater, times(1)).validateXmlElseUpdate(dailyListEntity);
            verifyNoInteractions(detsApiService);
            assertThat(dailyListEntity.getXmlContent()).isNull();
        }

        @Test
        void failedToDownloadFileFromDebts() throws Exception {
            UUID externalLocation = UUID.randomUUID();
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setExternalLocation(externalLocation);
            ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
            externalLocationTypeEntity.setId(4);
            dailyListEntity.setExternalLocationTypeEntity(externalLocationTypeEntity);

            doThrow(new RuntimeException()).when(detsApiService).downloadData(any());

            assertThat(dailyListUpdater.validateXmlElseUpdate(dailyListEntity))
                .isFalse();

            verify(dailyListUpdater, times(1)).validateXmlElseUpdate(dailyListEntity);
            verify(detsApiService, times(1)).downloadData(externalLocation);
            assertThat(dailyListEntity.getXmlContent()).isNull();
        }

        @Test
        //False positive
        @SuppressWarnings("PMD.CloseResource")
        void positive() throws Exception {
            final String xml = "<has_xml>true</has_xml";
            UUID externalLocation = UUID.randomUUID();
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setExternalLocation(externalLocation);
            ExternalLocationTypeEntity externalLocationTypeEntity = new ExternalLocationTypeEntity();
            externalLocationTypeEntity.setId(4);
            dailyListEntity.setExternalLocationTypeEntity(externalLocationTypeEntity);

            DownloadResponseMetaData downloadResponseMetaData = mock(DownloadResponseMetaData.class);
            Resource resource = mock(Resource.class);
            doReturn(resource).when(downloadResponseMetaData).getResource();
            doReturn(xml).when(resource).getContentAsString(any());
            doReturn(downloadResponseMetaData).when(detsApiService).downloadData(any());

            assertThat(dailyListUpdater.validateXmlElseUpdate(dailyListEntity))
                .isTrue();

            verify(dailyListUpdater, times(1)).validateXmlElseUpdate(dailyListEntity);
            verify(detsApiService, times(1)).downloadData(externalLocation);
            assertThat(dailyListEntity.getXmlContent()).isEqualTo(xml);
        }
    }

    @Nested
    class MapXmlToJson {

        @Test
        void xmlUnmarshalFailed() throws Exception {
            final String xml = "<has_xml>true</has_xml";
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setXmlContent(xml);

            doThrow(new RuntimeException()).when(xmlParser).unmarshal(any(), any());

            assertThat(dailyListUpdater.mapXmlToJson(dailyListEntity))
                .isFalse();

            verify(xmlParser, times(1))
                .unmarshal(xml, DailyListStructure.class);
            assertThat(dailyListEntity.getContent()).isNull();
        }

        @Test
        void xmlUnmarshalPassedObjectMappingFailed() throws Exception {
            final String xml = "<has_xml>true</has_xml";
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setXmlContent(xml);
            DailyListStructure dailyListStructure = mock(DailyListStructure.class);
            doReturn(dailyListStructure).when(xmlParser).unmarshal(any(), any());
            doThrow(new RuntimeException()).when(dailyListRequestMapper).mapToEntity(any());

            assertThat(dailyListUpdater.mapXmlToJson(dailyListEntity))
                .isFalse();

            verify(xmlParser, times(1))
                .unmarshal(xml, DailyListStructure.class);
            verify(dailyListRequestMapper, times(1))
                .mapToEntity(dailyListStructure);
            assertThat(dailyListEntity.getContent()).isNull();
        }

        @Test
        void xmlUnmarshalPassedJsonMappingFailed() throws Exception {
            final String xml = "<has_xml>true</has_xml";
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setXmlContent(xml);
            DailyListStructure dailyListStructure = mock(DailyListStructure.class);
            doReturn(dailyListStructure).when(xmlParser).unmarshal(any(), any());
            ObjectMapper objectMapper = mock(ObjectMapper.class);
            doReturn(objectMapper).when(dailyListUpdater).getServiceObjectMapper();

            doThrow(new RuntimeException()).when(objectMapper).writeValueAsString(any());

            DailyListJsonObject modernisedDailyList = mock(DailyListJsonObject.class);
            doReturn(modernisedDailyList).when(dailyListRequestMapper).mapToEntity(any());

            assertThat(dailyListUpdater.mapXmlToJson(dailyListEntity))
                .isFalse();

            verify(xmlParser, times(1))
                .unmarshal(xml, DailyListStructure.class);
            verify(dailyListRequestMapper, times(1))
                .mapToEntity(dailyListStructure);
            verify(dailyListUpdater, times(1))
                .getServiceObjectMapper();
            verify(objectMapper, times(1))
                .writeValueAsString(modernisedDailyList);
            assertThat(dailyListEntity.getContent()).isNull();
        }

        @Test
        void xmlUnmarshalPassedJsonMappingPassed() throws Exception {
            final String xml = "<has_xml>true</has_xml";
            DailyListEntity dailyListEntity = new DailyListEntity();
            dailyListEntity.setXmlContent(xml);
            DailyListStructure dailyListStructure = mock(DailyListStructure.class);
            doReturn(dailyListStructure).when(xmlParser).unmarshal(any(), any());
            ObjectMapper objectMapper = mock(ObjectMapper.class);
            doReturn(objectMapper).when(dailyListUpdater).getServiceObjectMapper();

            final String json = "{\"has_json\": true}";
            doReturn(json).when(objectMapper).writeValueAsString(any());

            DailyListJsonObject modernisedDailyList = mock(DailyListJsonObject.class);
            doReturn(modernisedDailyList).when(dailyListRequestMapper).mapToEntity(any());

            assertThat(dailyListUpdater.mapXmlToJson(dailyListEntity))
                .isTrue();

            verify(xmlParser, times(1))
                .unmarshal(xml, DailyListStructure.class);
            verify(dailyListRequestMapper, times(1))
                .mapToEntity(dailyListStructure);
            verify(dailyListUpdater, times(1))
                .getServiceObjectMapper();
            verify(objectMapper, times(1))
                .writeValueAsString(modernisedDailyList);
            assertThat(dailyListEntity.getContent()).isEqualTo(json);

        }
    }
}