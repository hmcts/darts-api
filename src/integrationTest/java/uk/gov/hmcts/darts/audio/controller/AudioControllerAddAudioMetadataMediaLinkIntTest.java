package uk.gov.hmcts.darts.audio.controller;

import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.util.unit.DataSize;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.service.AudioAsyncService;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.MediaLinkedCaseEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.MediaLinkedCaseSourceType;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.test.common.DataGenerator;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.EventStub;
import uk.gov.hmcts.darts.testutils.stubs.HearingStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.hmcts.darts.test.common.AwaitabilityUtil.waitForMaxWithOneSecondPoll;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.servlet.multipart.max-file-size=4MB",
    "spring.servlet.multipart.max-request-size=4MB",
})
class AudioControllerAddAudioMetadataMediaLinkIntTest extends IntegrationBase {

    private static final URI ENDPOINT = URI.create("/audios");
    private static final OffsetDateTime STARTED_AT =
        OffsetDateTime.of(2025, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final Path AUDIO_BINARY_PAYLOAD_1 = DataGenerator.createUniqueFile(DataSize.ofBytes(20), DataGenerator.FileType.MP2);

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthorisationStub authorisationStub;
    @Autowired
    private EventStub eventStub;
    @Autowired
    private HearingStub hearingStub;
    @MockBean
    private UserIdentity mockUserIdentity;

    @Autowired
    AudioAsyncService audioAsyncService;

    @Autowired
    private SuperAdminUserStub superAdminUserStub;


    @BeforeEach
    void beforeEach() {
        openInViewUtil.openEntityManager();
        authorisationStub.givenTestSchema();

        UserAccountEntity testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);
        dartsDatabase.getUserAccountRepository().save(testUser);
    }

    @AfterEach
    void closeHibernateSession() {
        openInViewUtil.closeEntityManager();
    }

    @Test
    void addAudioMetadataDifferentCasesAndMediaLink() throws Exception {
        // given
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        dartsDatabase.createCase("Bristol", "case1");
        dartsDatabase.createCase("Bristol", "case2");
        dartsDatabase.createCase("Bristol", "case3");

        HearingEntity hearingForEvent =
            hearingStub.createHearing("Bristol", "1", "case1", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingForEvent, 10, STARTED_AT.minusMinutes(20), "LOG");
        HearingEntity hearingDifferentCourtroom =
            hearingStub.createHearing("Bristol", "2", "case2", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingDifferentCourtroom, 10, STARTED_AT.minusMinutes(20), "LOG");
        HearingEntity hearingAfter =
            hearingStub.createHearing("Bristol", "1", "case3", DateConverterUtil.toLocalDateTime(STARTED_AT));
        eventStub.createEvent(hearingAfter, 10, STARTED_AT.plusMinutes(20), "LOG");

        // when
        makeAddAudioCall(AUDIO_BINARY_PAYLOAD_1, "case1")
            .andExpect(status().isOk());

        // then
        List<HearingEntity> allHearings =
            dartsDatabase.getHearingRepository().findByCourthouseCourtroomAndDate("bristol", "1", STARTED_AT.toLocalDate());

        HearingEntity caseAHearing = allHearings.stream()
            .filter(hearingEntity -> "case1".equals(hearingEntity.getCourtCase().getCaseNumber())).findFirst().orElseThrow();

        List<MediaEntity> mediaEntities = dartsDatabase.getMediaRepository().findAllCurrentMediaByHearingId(caseAHearing.getId());
        assertEquals(1, mediaEntities.size());
        MediaEntity media = mediaEntities.getFirst();
        assertEquals(STARTED_AT, media.getStart());
        assertEquals(STARTED_AT, media.getEnd());

        List<MediaLinkedCaseEntity> mediaLinkedCaseEntities = dartsDatabase.getMediaLinkedCaseRepository().findByMedia(media);
        waitForMaxWithOneSecondPoll(
            () -> assertEquals(2, mediaLinkedCaseEntities.size()),
            Duration.ofSeconds(20)
        );
        MediaLinkedCaseEntity case1Link = mediaLinkedCaseEntities.getFirst();
        assertEquals(MediaLinkedCaseSourceType.ADD_AUDIO_METADATA, case1Link.getSource());
        assertEquals("case1", case1Link.getCourtCase().getCaseNumber());

        MediaLinkedCaseEntity case3Link = mediaLinkedCaseEntities.get(1);
        assertEquals(MediaLinkedCaseSourceType.ADD_AUDIO_EVENT_LINKING, case3Link.getSource());
        assertEquals("case3", case3Link.getCourtCase().getCaseNumber());
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, Path audioBinaryPayload,
                                                          String... casesList) throws IOException {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom,
                                     "mp2", audioBinaryPayload, casesList);
    }

    private AddAudioMetadataRequest createAddAudioRequest(OffsetDateTime startedAt, OffsetDateTime endedAt,
                                                          String courthouse, String courtroom, String filetype, Path audioBinaryPayload,
                                                          String... casesList) throws IOException {

        AddAudioMetadataRequest addAudioMetadataRequest = new AddAudioMetadataRequest();
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
        return addAudioMetadataRequest;
    }

    @SneakyThrows
    private ResultActions makeAddAudioCall(Path audioBinaryPayload, String... casesToMapTo) {

        AddAudioMetadataRequest addAudioMetadataRequest =
            createAddAudioRequest(STARTED_AT, STARTED_AT, "Bristol", "1", audioBinaryPayload, casesToMapTo);

        MockMultipartFile audioFile = new MockMultipartFile(
            "file",
            "test_audio.mp2",
            "audio/mpeg",
            IOUtils.toByteArray(Files.newInputStream(audioBinaryPayload)));

        MockMultipartFile metadataJson = new MockMultipartFile(
            "metadata",
            null,
            "application/json",
            objectMapper.writeValueAsString(addAudioMetadataRequest).getBytes()
        );

        return mockMvc.perform(
            multipart(ENDPOINT)
                .file(audioFile)
                .file(metadataJson));
    }
}