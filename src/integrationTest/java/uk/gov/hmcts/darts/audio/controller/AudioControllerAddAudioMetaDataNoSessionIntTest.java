package uk.gov.hmcts.darts.audio.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.util.unit.DataSize;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequest;
import uk.gov.hmcts.darts.audio.model.AddAudioMetadataRequestWithStorageGUID;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.enums.SecurityRoleEnum;
import uk.gov.hmcts.darts.test.common.DataGenerator;
import uk.gov.hmcts.darts.testutils.IntegrationBase;
import uk.gov.hmcts.darts.testutils.stubs.AuthorisationStub;
import uk.gov.hmcts.darts.testutils.stubs.SuperAdminUserStub;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.servlet.multipart.max-file-size=4MB",
    "spring.servlet.multipart.max-request-size=4MB",
})
@SuppressWarnings({"PMD.DoNotUseThreads", "PMD.AvoidInstantiatingObjectsInLoops", "PMD.AvoidThrowingRawExceptionTypes"})
class AudioControllerAddAudioMetaDataNoSessionIntTest extends IntegrationBase {

    @Value("${local.server.port}")
    protected int port;

    private static final URI ENDPOINT = URI.create("/audios/metadata");
    private static final OffsetDateTime STARTED_AT = OffsetDateTime.of(2024, 10, 10, 10, 0, 0, 0, ZoneOffset.UTC);
    private static final Path AUDIO_BINARY_PAYLOAD_1 = DataGenerator.createUniqueFile(DataSize.ofBytes(10), DataGenerator.FileType.MP2);


    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AuthorisationStub authorisationStub;
    @MockBean
    private UserIdentity mockUserIdentity;
    @Autowired
    private SuperAdminUserStub superAdminUserStub;


    @BeforeEach
    void beforeEach() {
        UserAccountEntity testUser = authorisationStub.getTestUser();
        when(mockUserIdentity.getUserAccount()).thenReturn(testUser);

        dartsDatabase.createCourthouseUnlessExists("Bristol");

    }


    @Test
    void testAddAudioWithConcurrency() throws Exception {
        superAdminUserStub.givenUserIsAuthorised(mockUserIdentity, SecurityRoleEnum.MID_TIER);

        int numberOfThreads = 5;
        try (ExecutorService service = Executors.newFixedThreadPool(5)) {
            CountDownLatch latch = new CountDownLatch(numberOfThreads);
            List<Future<?>> futures = new ArrayList<>();

            for (int i = 0; i < numberOfThreads; i++) {
                final int threadNum = i;
                Future<?> future = service.submit(() -> {
                    try {
                        OffsetDateTime startTime = STARTED_AT.plusDays(threadNum);
                        AddAudioMetadataRequest addAudioMetadataRequest = createAddAudioRequest(startTime, startTime.plusHours(1), "Bristol", "1");
                        mockMvc.perform(post(ENDPOINT)
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(objectMapper.writeValueAsString(addAudioMetadataRequest)))
                            .andDo(MockMvcResultHandlers.print())
                            .andExpect(status().isOk())
                            .andReturn();

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    } finally {
                        latch.countDown();
                    }
                });
                futures.add(future);
            }

            // Wait for all tasks to complete
            boolean completed = latch.await(5, TimeUnit.SECONDS);
            assertTrue(completed, "Not all threads completed in time");

            // Wait for all futures to complete to ensure DB operations are done
            for (Future<?> future : futures) {
                future.get();
            }

            // Add a small delay to allow for any potential lag in DB updates
            Thread.sleep(100);

            List<MediaEntity> mediaList = dartsDatabase.getMediaRepository().findAll();

            assertEquals(numberOfThreads, mediaList.size());
        }
    }

    private AddAudioMetadataRequestWithStorageGUID createAddAudioRequest(OffsetDateTime startedAt,
                                                                         OffsetDateTime endedAt, String courthouse, String courtroom) throws IOException {
        return createAddAudioRequest(startedAt, endedAt, courthouse, courtroom,
                                     "mp2", AUDIO_BINARY_PAYLOAD_1, "case1", "case2", "case3");
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
        addAudioMetadataRequest.storageGuid(UUID.randomUUID());
        addAudioMetadataRequest.setChecksum("checksum-" + addAudioMetadataRequest.getStorageGuid());
        return addAudioMetadataRequest;
    }
}