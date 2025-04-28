package uk.gov.hmcts.darts.audio.controller;

import org.apache.logging.log4j.util.TriConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.unit.DataSize;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequestWithStorageGUID;
import uk.gov.hmcts.darts.audio.model.Problem;
import uk.gov.hmcts.darts.audio.service.AudioAsyncService;
import uk.gov.hmcts.darts.audio.service.AudioUploadService;
import uk.gov.hmcts.darts.audit.api.AuditActivity;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.AuditEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.ObjectAdminActionEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.repository.AuditRepository;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.DataGenerator;
import uk.gov.hmcts.darts.test.common.data.MediaTestData;
import uk.gov.hmcts.darts.test.common.data.PersistableFactory;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AudioControllerAddAudioMetadataIntTest extends IntegrationBase {

    @Value("${local.server.port}")
    protected int port;

    private static final URI ENDPOINT = URI.create("/audios/metadata");
    private static final OffsetDateTime STARTED_AT = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final Path AUDIO_BINARY_PAYLOAD_1 = DataGenerator.createUniqueFile(DataSize.ofBytes(10),
                                                                                      DataGenerator.FileType.MP2);
    private static final Path AUDIO_BINARY_PAYLOAD_2 = DataGenerator.createUniqueFile(DataSize.ofBytes(10),
                                                                                      DataGenerator.FileType.MP2);
    private static final Path AUDIO_BINARY_PAYLOAD_3 = DataGenerator.createUniqueFile(DataSize.ofBytes(10),
                                                                                      DataGenerator.FileType.MP2);
    private static final Path AUDIO_BINARY_PAYLOAD_EXCEEDING_MAX_ALLOWABLE_SIZE = DataGenerator.createUniqueFile(DataSize.ofMegabytes(5),
                                                                                                                 DataGenerator.FileType.MP2);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private EventStub eventStub;
    @Autowired
    HearingStub hearingStub;
    @MockitoBean
    private UserIdentity mockUserIdentity;

    @MockitoBean
    AudioAsyncService audioAsyncService;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;

    @Autowired
    private AuditRepository auditRepository;

    private String guid = UUID.randomUUID().toString();

    private static final long END_FILE_DURATION = 1440;
    @Autowired
    private AudioUploadService audioUploadService;


    @BeforeEach
    void beforeEach() {
        openInViewUtil.openEntityManager();
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        dartsDatabase.getUserAccountRepository().save(testUser);

        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        HearingEntity hearingForEvent = hearingStub.createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingForEvent, 10, STARTED_AT.minusMinutes(20), "LOG");
        HearingEntity hearingDifferentCourtroom = hearingStub.createHearing("Bristol", "2", "case2", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingDifferentCourtroom, 10, STARTED_AT.minusMinutes(20), "LOG");
    }

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void addAudioMetadata() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plusMinutes(END_FILE_DURATION), "Bristol", "1");

        mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addAudioMetadataRequest)))
            .andExpect(status().isOk())
            .andReturn();

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearing.getId());
            MediaEntity media = mediaEntities.getFirst();
            assertEquals(1, mediaEntities.size());
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(STARTED_AT.plusMinutes(END_FILE_DURATION), media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = dartsDatabase.getMediaLinkedCaseRepository().findByMedia(media);
            assertEquals(3, mediaLinkedCaseEntities.size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(media.getId().toString(), media.getChronicleId());
            assertEquals(true, media.getIsCurrent());
            assertNull(media.getAntecedentId());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom

        HearingEntity hearingEntity = hearingsInAnotherCourtroom.getFirst();
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom
    }

    @Test
    void addAudioMetadataChecksumsDoNotMatch() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequestWithStorageGUID addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plusMinutes(END_FILE_DURATION),
                                                                                               "Bristol", "1");
        addAudioMetadataRequest.setChecksum("invalidchecksum");
        MvcResult mvcResult = mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addAudioMetadataRequest)))
            .andExpect(status().isUnprocessableEntity())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();

        Problem problem = objectMapper.readValue(actualJson, Problem.class);
        assertEquals(AudioApiError.FAILED_TO_ADD_AUDIO_META_DATA.getType(), problem.getType().toString());
        assertEquals(AudioApiError.FAILED_TO_ADD_AUDIO_META_DATA.getTitle(), problem.getTitle());
        assertEquals(
            "Checksum for blob 'checksum-" + addAudioMetadataRequest.getStorageGuid()
                + "' does not match the one passed in the API request 'invalidchecksum'.",
            problem.getDetail());
    }

    @Test
    void addAudioMetadataDuplicate() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        makeAddAudioCall();
        makeAddAudioCall();

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearing.getId());
            MediaEntity media = mediaEntities.getFirst();
            assertEquals(1, mediaEntities.size());
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(STARTED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = dartsDatabase.getMediaLinkedCaseRepository().findByMedia(media);
            assertEquals(3, mediaLinkedCaseEntities.size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(media.getId().toString(), media.getChronicleId());
            assertNull(media.getAntecedentId());
            assertEquals(media.getId().toString(), media.getChronicleId());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom

        HearingEntity hearingEntity = hearingsInAnotherCourtroom.getFirst();
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom
    }

    @Test
    void addAudioMetadataVersionedDueToDuplicateMetadataButDifferentChecksum() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        makeAddAudioCall();

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository()
            .findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        MediaEntity originalMedia = null;
        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearing.getId());

            assertEquals(1, mediaEntities.size());

            originalMedia = mediaEntities.getFirst();
            assertEquals(STARTED_AT, originalMedia.getStart());
            assertEquals(STARTED_AT, originalMedia.getEnd());
            assertEquals(1, originalMedia.getChannel());
            assertEquals(2, originalMedia.getTotalChannels());
            List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = dartsDatabase.getMediaLinkedCaseRepository().findByMedia(originalMedia);
            assertEquals(3, mediaLinkedCaseEntities.size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(originalMedia.getCourtroom().getId()).get().getName());
            assertEquals(originalMedia.getId().toString(), originalMedia.getChronicleId());
            assertNull(originalMedia.getAntecedentId());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom

        guid = UUID.randomUUID().toString();
        Long newMedia = uploadAnotherAudioWithSize(AUDIO_BINARY_PAYLOAD_2, originalMedia.getId().toString(), originalMedia.getId().toString());
        guid = UUID.randomUUID().toString();
        Long newMedia2 = uploadAnotherAudioWithSize(AUDIO_BINARY_PAYLOAD_3, newMedia.toString(), originalMedia.getId().toString());
        assertNotEquals(newMedia, newMedia2);
        Optional<MediaEntity> newMediaEntity = dartsDatabase.getMediaRepository().findById(newMedia);
        assertEquals(false, newMediaEntity.get().getIsCurrent());
        Optional<MediaEntity> newMedia2Entity = dartsDatabase.getMediaRepository().findById(newMedia2);
        assertEquals(true, newMedia2Entity.get().getIsCurrent());

    }

    @Test
    void addAudioMetadataNonExistingCourthouse() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plusMinutes(END_FILE_DURATION), "TEST", "1");

        MvcResult mvcResult = mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addAudioMetadataRequest)))
            .andExpect(status().isNotFound())
            .andReturn();

        String actualJson = mvcResult.getResponse().getContentAsString();
        String expectedJson = """
            {"type":"COMMON_100","title":"Provided courthouse does not exist","status":404,"detail":"Courthouse 'TEST' not found."}""";

        JSONAssert.assertEquals(expectedJson, actualJson, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void addAudioReturnForbiddenError() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.DAR_PC);

        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plusMinutes(END_FILE_DURATION), "TEST", "1");


        MvcResult mvcResult = mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addAudioMetadataRequest)))
            .andExpect(status().isForbidden())
            .andReturn();

        String actualResponse = mvcResult.getResponse().getContentAsString();

        String expectedResponse = """
            {"type":"AUTHORISATION_109","title":"User is not authorised for this endpoint","status":403}
            """;
        JSONAssert.assertEquals(expectedResponse, actualResponse, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldHideIncomingMedia_whenIncomingMediaHasExistingVersionThatIsHiddenButHasNoExistingAdminAction() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        CourtroomEntity existingCourtroom = PersistableFactory.getCourtroomTestData().someMinimalBuilderHolder().getBuilder()
            .courthouse(PersistableFactory.getCourthouseTestData().someMinimal())
            .build()
            .getEntity();
        dartsPersistence.save(existingCourtroom);

        final OffsetDateTime startAt = OffsetDateTime.parse("2024-10-10T10:00:00Z");
        final OffsetDateTime endAt = OffsetDateTime.parse("2024-10-10T10:15:00Z");
        MediaEntity initialMedia = PersistableFactory.getMediaTestData().someMinimalBuilderHolder()
            .getBuilder()
            .isHidden(true)
            // The following attributes must align with the data that gets created by createAddAudioRequest(), so that we get a duplicate metadata scenario
            .courtroom(existingCourtroom)
            .channel(1)
            .mediaFile("test")
            .start(startAt)
            .end(endAt)
            .build()
            .getEntity();
        dartsPersistence.save(initialMedia);
        String chronicleId = initialMedia.getId().toString();
        initialMedia.setChronicleId(chronicleId);
        dartsPersistence.save(initialMedia);

        AddAudioMetadataRequest request = createAddAudioRequest(startAt,
                                                                endAt,
                                                                existingCourtroom.getCourthouse().getCourthouseName(),
                                                                existingCourtroom.getName(),
                                                                AUDIO_BINARY_PAYLOAD_1);

        // When
        mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        // Then, assert DB state
        List<MediaEntity> allMedias = dartsDatabase.getMediaRepository().findAll();

        List<MediaEntity> allVersions = allMedias.stream()
            .filter(media -> chronicleId.equals(media.getChronicleId()))
            .toList();
        assertEquals(2, allVersions.size());
        assertTrue(allVersions.stream().allMatch(MediaEntity::isHidden));

        // Identify the newly added version
        List<MediaEntity> newMediaVersions = allMedias.stream()
            .filter(media -> String.valueOf(initialMedia.getId()).equals(media.getAntecedentId()))
            .toList();
        assertEquals(1, newMediaVersions.size());
        MediaEntity newMediaVersion = newMediaVersions.getFirst();

        Optional<ObjectAdminActionEntity> adminActionOptional = newMediaVersion.getObjectAdminAction();
        assertTrue(adminActionOptional.isPresent());
        ObjectAdminActionEntity adminAction = adminActionOptional.get();
        assertNull(adminAction.getTicketReference());
        assertEquals("Prior version had no admin action, so no details are available", adminAction.getComments());
        assertNull(adminAction.getObjectHiddenReason());

        List<AuditEntity> hideAudio = dartsDatabase.getAuditRepository().findAll().stream()
            .filter(audit -> AuditActivity.HIDE_AUDIO.getId().equals(audit.getAuditActivity().getId()))
            .toList();
        assertEquals(1, hideAudio.size());
    }

    @Test
    void shouldHideIncomingMediaAndCopyExistingAdminAction_whenIncomingMediaHasExistingVersionThatIsHiddenAndHasExistingAdminAction() throws Exception {
        // Given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        CourtroomEntity existingCourtroom = PersistableFactory.getCourtroomTestData().someMinimalBuilderHolder().getBuilder()
            .courthouse(PersistableFactory.getCourthouseTestData().someMinimal())
            .build()
            .getEntity();
        dartsPersistence.save(existingCourtroom);

        final OffsetDateTime startAt = OffsetDateTime.parse("2024-10-10T10:00:00Z");
        final OffsetDateTime endAt = OffsetDateTime.parse("2024-10-10T10:15:00Z");
        MediaEntity initialMedia = PersistableFactory.getMediaTestData().someMinimalBuilderHolder()
            .getBuilder()
            .isHidden(true)
            // The following attributes must align with the data that gets created by createAddAudioRequest(), so that we get a duplicate metadata scenario
            .courtroom(existingCourtroom)
            .channel(1)
            .mediaFile("test")
            .start(startAt)
            .end(endAt)
            .build()
            .getEntity();
        dartsPersistence.save(initialMedia);

        ObjectAdminActionEntity adminActionForInitialMedia = PersistableFactory.getObjectAdminActionTestData().someMinimalBuilderHolder()
            .getBuilder()
            .ticketReference("Some ticket ref")
            .comments("Some comments")
            .media(initialMedia)
            .build()
            .getEntity();
        dartsPersistence.save(adminActionForInitialMedia);

        String chronicleId = initialMedia.getId().toString();
        initialMedia.setChronicleId(chronicleId);
        initialMedia.setObjectAdminAction(adminActionForInitialMedia);
        dartsPersistence.save(initialMedia);

        AddAudioMetadataRequest request = createAddAudioRequest(startAt,
                                                                endAt,
                                                                existingCourtroom.getCourthouse().getCourthouseName(),
                                                                existingCourtroom.getName(),
                                                                AUDIO_BINARY_PAYLOAD_1);

        // When
        mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk());

        // Then, assert DB state
        List<MediaEntity> allMedias = dartsDatabase.getMediaRepository().findAll();

        List<MediaEntity> allVersions = allMedias.stream()
            .filter(media -> chronicleId.equals(media.getChronicleId()))
            .toList();
        assertEquals(2, allVersions.size());
        assertTrue(allVersions.stream().allMatch(MediaEntity::isHidden));

        // Identify the newly added version
        List<MediaEntity> newMediaVersions = allMedias.stream()
            .filter(media -> String.valueOf(initialMedia.getId()).equals(media.getAntecedentId()))
            .toList();
        assertEquals(1, newMediaVersions.size());
        MediaEntity newMediaVersion = newMediaVersions.getFirst();

        Optional<ObjectAdminActionEntity> adminActionOptional = newMediaVersion.getObjectAdminAction();
        assertTrue(adminActionOptional.isPresent());
        ObjectAdminActionEntity adminAction = adminActionOptional.get();
        assertEquals(adminActionForInitialMedia.getTicketReference(), adminAction.getTicketReference());
        assertEquals(adminActionForInitialMedia.getComments(), adminAction.getComments());

        List<AuditEntity> hideAudio = dartsDatabase.getAuditRepository().findAll().stream()
            .filter(audit -> AuditActivity.HIDE_AUDIO.getId().equals(audit.getAuditActivity().getId()))
            .toList();
        assertEquals(1, hideAudio.size());
    }


    @Test
    void whenAddAudioIsCalled_withTheLastCurrentValueNotBeingTheLastAntecendntValue_shouldUseLastCurrentValueForDuplicateChecks() throws Exception {
        UserAccountEntity userAccount = superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);
        final AddAudioMetadataRequestWithStorageGUID addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT.plusMinutes(END_FILE_DURATION),
                                                                                                     "Bristol", "1");

        final String oldVersionChecksum = UUID.randomUUID().toString();
        final CourtroomEntity courtroomEntity = dartsDatabase.getRetrieveCoreObjectService().retrieveOrCreateCourtroom(addAudioMetadataRequest.getCourthouse(),
                                                                                                                       addAudioMetadataRequest.getCourtroom(),
                                                                                                                       userAccount);

        final Function<MediaEntity, MediaEntity> alignRequest = (media) -> {
            media.setCourtroom(courtroomEntity);
            media.setChannel(addAudioMetadataRequest.getChannel());
            media.setMediaFile(addAudioMetadataRequest.getFilename());
            media.setStart(addAudioMetadataRequest.getStartedAt());
            media.setEnd(addAudioMetadataRequest.getEndedAt());
            media.setChecksum(oldVersionChecksum);
            media.setIsCurrent(false);
            return dartsDatabase.save(media);
        };
        final TriConsumer<Long, MediaEntity, MediaEntity> alignDataMediaEntity = (chronicleId, previousMedia, newMediaEntity) -> {
            newMediaEntity.setChronicleId(String.valueOf(chronicleId));
            newMediaEntity.setAntecedentId(String.valueOf(previousMedia.getId()));
        };

        /*
        Values pulled from staging which caused the original failure (These values are replicated in this test)
        TestId, m.chronicle_id, m.antecedent_id, m.created_ts, m.is_current,m.checksum, m.med_id
        MED5: 41017 41077 2025-04-02 11:15:22.382 +0100 false  47f13e9b2248d73730e165a50fb8c395 162825
        MED4: 41017 41037 2024-05-16 16:54:36.551 +0100 false  81ef8524d69c7ae6605baf37e425b574 41057
        MED3: 41017 41057 2024-05-16 16:53:34.321 +0100 false  81ef8524d69c7ae6605baf37e425b573 41077
        MED2: 41017 41017 2024-05-16 16:49:47.107 +0100 true   1ef8524d69c7ae6605baf37e425b572  41037
        MED1: 41017       2024-05-16 16:49:34.247 +0100 false  81ef8524d69c7ae6605baf37e425b571 41017

        chronicle_id & antecedent_id mapped to media variable names instead of id's for clarity
        MED5: MED1 MED3 2025-04-02 11:15:22.382 +0100 fals 47f13e9b2248d73730e165a50fb8c395 162825
        MED4: MED1 MED2 2024-05-16 16:54:36.551 +0100 fals 81ef8524d69c7ae6605baf37e425b574 41057
        MED3: MED1 MED4 2024-05-16 16:53:34.321 +0100 fals 81ef8524d69c7ae6605baf37e425b573 41077
        MED2: MED1 MED1 2024-05-16 16:49:47.107 +0100 true 81ef8524d69c7ae6605baf37e425b572 41037
        MED1: MED1      2024-05-16 16:49:34.247 +0100 fals 81ef8524d69c7ae6605baf37e425b571 41017
        */

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS").withZone(ZoneOffset.UTC);

        MediaEntity media1 = alignRequest.apply(new MediaTestData().someMinimal());
        media1.setCreatedDateTime(OffsetDateTime.parse("2024-05-16 16:49:34.247", dateTimeFormatter));
        MediaEntity media2 = alignRequest.apply(new MediaTestData().someMinimal());
        media2.setCreatedDateTime(OffsetDateTime.parse("2024-05-16 16:49:47.107", dateTimeFormatter));
        MediaEntity media3 = alignRequest.apply(new MediaTestData().someMinimal());
        media3.setCreatedDateTime(OffsetDateTime.parse("2024-05-16 16:53:34.321", dateTimeFormatter));
        MediaEntity media4 = alignRequest.apply(new MediaTestData().someMinimal());
        media4.setCreatedDateTime(OffsetDateTime.parse("2024-05-16 16:54:36.551", dateTimeFormatter));
        MediaEntity media5 = alignRequest.apply(new MediaTestData().someMinimal());
        media5.setCreatedDateTime(OffsetDateTime.parse("2025-04-02 11:15:22.382", dateTimeFormatter));

        media1.setChronicleId(String.valueOf(media1.getId()));
        alignDataMediaEntity.accept(media1.getId(), media1, media2);
        alignDataMediaEntity.accept(media1.getId(), media4, media3);
        alignDataMediaEntity.accept(media1.getId(), media2, media4);
        alignDataMediaEntity.accept(media1.getId(), media3, media5);
        media2.setIsCurrent(true);

        dartsDatabase.getMediaRepository().saveAndFlush(media1);
        dartsDatabase.getMediaRepository().saveAndFlush(media2);
        dartsDatabase.getMediaRepository().saveAndFlush(media3);
        dartsDatabase.getMediaRepository().saveAndFlush(media4);
        dartsDatabase.getMediaRepository().saveAndFlush(media5);

        mockMvc.perform(
                post(ENDPOINT)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(addAudioMetadataRequest)))
            .andExpect(status().isOk())
            .andReturn();

        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAll()
            .stream()
            .filter(media -> oldVersionChecksum.equals(media.getChecksum()) || addAudioMetadataRequest.getChecksum().equals(media.getChecksum()))
            .sorted((o1, o2) -> o2.getCreatedDateTime().compareTo(o1.getCreatedDateTime()))
            .toList();
        assertThat(mediaEntities).hasSize(6);
        //There should only be one current media
        assertThat(mediaEntities.get(0).getIsCurrent()).isTrue();
        assertThat(mediaEntities.get(1).getIsCurrent()).isFalse();
        assertThat(mediaEntities.get(2).getIsCurrent()).isFalse();
        assertThat(mediaEntities.get(3).getIsCurrent()).isFalse();
        assertThat(mediaEntities.get(4).getIsCurrent()).isFalse();
        assertThat(mediaEntities.get(5).getIsCurrent()).isFalse();

        assertThat(mediaEntities.get(0).getAntecedentId()).isEqualTo(String.valueOf(media5.getId()));
        assertThat(mediaEntities.get(1).getAntecedentId()).isEqualTo(String.valueOf(media3.getId()));
        assertThat(mediaEntities.get(2).getAntecedentId()).isEqualTo(String.valueOf(media2.getId()));
        assertThat(mediaEntities.get(3).getAntecedentId()).isEqualTo(String.valueOf(media4.getId()));
        assertThat(mediaEntities.get(4).getAntecedentId()).isEqualTo(String.valueOf(media1.getId()));
        assertThat(mediaEntities.get(5).getAntecedentId()).isNull();
    }

    private AddAudioMetadataRequestWithStorageGUID createAddAudioRequest(OffsetDateTime startedAt,
                                                                         OffsetDateTime endedAt, String courthouse, String courtroom) throws IOException {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom,
                                     "mp2", AUDIO_BINARY_PAYLOAD_1, "case1", "case2", "case3");
    }


    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt,
                                                          OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, Path audioBinaryPayload) throws IOException {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom, "mp2", audioBinaryPayload, "case1", "case2", "case3");
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, Path audioBinaryPayload,
                                                          String... casesList) throws IOException {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom,
                                     "mp2", audioBinaryPayload, casesList);
    }

    private AddAudioMetadataRequestWithStorageGUID createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                                                         String courthouse, String courtroom, String filetype, Path audioBinaryPayload,
                                                                         String... casesList) throws IOException {

        AddAudioMetadataRequestWithStorageGUID addAudioMetadataRequest = new AddAudioMetadataRequestWithStorageGUID();
        addAudioMetadataRequest.startedAt(startedAt);
        addAudioMetadataRequest.endedAt(endedAt);
        addAudioMetadataRequest.setChannel(1);
        addAudioMetadataRequest.totalChannels(2);
        addAudioMetadataRequest.format(filetype);
        addAudioMetadataRequest.filename("test");
        addAudioMetadataRequest.courthouse(courthouse);
        addAudioMetadataRequest.courtroom(courtroom);
        addAudioMetadataRequest.cases(List.of(casesList));
        addAudioMetadataRequest.setMediaFile("media file");
        addAudioMetadataRequest.setFileSize(Files.size(audioBinaryPayload));
        addAudioMetadataRequest.setChecksum("calculatedchecksum");
        addAudioMetadataRequest.storageGuid(UUID.fromString(guid));
        addAudioMetadataRequest.setChecksum("checksum-" + guid);
        return addAudioMetadataRequest;
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    private ResultActions makeAddAudioCall() throws Exception {
        return makeAddAudioCall(AUDIO_BINARY_PAYLOAD_1);
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    private ResultActions makeAddAudioCall(Path audioBinaryPayload, String... casesToMapTo) throws Exception {
        UserAccountEntity testUser = authorisationStub.getSystemUser();
        dartsDatabase.getUserAccountRepository().save(testUser);

        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        HearingEntity hearingForEvent = hearingStub.createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingForEvent, 10, STARTED_AT.minusMinutes(20), "LOG");
        HearingEntity hearingDifferentCourtroom = hearingStub.createHearing("Bristol", "2", "case2", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingDifferentCourtroom, 10, STARTED_AT.minusMinutes(20), "LOG");
        HearingEntity hearingAfter = hearingStub.createHearing("Bristol", "1", "case3", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingAfter, 10, STARTED_AT.plusMinutes(20), "LOG");

        AddAudioMetadataRequest addAudioMetadataRequest;
        if (casesToMapTo.length == 0) {
            addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT, "Bristol", "1", audioBinaryPayload);
        } else {
            addAudioMetadataRequest = createAddAudioRequest(STARTED_AT, STARTED_AT, "Bristol", "1", audioBinaryPayload, casesToMapTo);
        }

        return mockMvc.perform(
            post(ENDPOINT)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addAudioMetadataRequest)));
    }

    @SuppressWarnings({"PMD.SignatureDeclareThrowsException"})
    private Long uploadAnotherAudioWithSize(Path audioBinaryPayload, String expectedAntecedantId, String expectedChronicleId) throws Exception {
        makeAddAudioCall(audioBinaryPayload)
            .andExpect(status().isOk());

        List<HearingEntity> allHearings = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        List<HearingEntity> addAudioLinkedHearings = new ArrayList<>();
        for (HearingEntity hearing : allHearings) {
            if (hearing.getCourtCase().getCaseNumber().contains("case")) {
                addAudioLinkedHearings.add(hearing);
            }
        }
        assertEquals(3, addAudioLinkedHearings.size());

        MediaEntity media = null;
        for (HearingEntity hearing : addAudioLinkedHearings) {
            List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearing.getId());

            assertEquals(1, mediaEntities.size());

            media = mediaEntities.getFirst();
            assertEquals(STARTED_AT, media.getStart());
            assertEquals(STARTED_AT, media.getEnd());
            assertEquals(1, media.getChannel());
            assertEquals(2, media.getTotalChannels());
            List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = dartsDatabase.getMediaLinkedCaseRepository().findByMedia(media);
            assertEquals(3, mediaLinkedCaseEntities.size());
            assertEquals("1", dartsDatabase.getCourtroomRepository().findById(media.getCourtroom().getId()).get().getName());
            assertEquals(expectedChronicleId, media.getChronicleId());
            assertEquals(expectedAntecedantId, media.getAntecedentId());
        }

        List<HearingEntity> hearingsInAnotherCourtroom = dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate(
            "bristol",
            "2",
            STARTED_AT.toLocalDate()
        );
        assertEquals(1, hearingsInAnotherCourtroom.size());//should have hearingDifferentCourtroom
        HearingEntity hearingEntity = hearingsInAnotherCourtroom.getFirst();
        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(hearingEntity.getId());
        assertEquals(0, mediaEntities.size());//shouldn't have any as no audio in that courtroom
        return media.getId();
    }

}