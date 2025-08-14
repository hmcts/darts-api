package uk.gov.hmcts.darts.audio.component.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.component.OutboundFileZipGeneratorHelper;
import uk.gov.hmcts.darts.audio.config.AudioConfigurationProperties;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.CourtroomEntity;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.EventHandlerEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.EventRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.ParserConfigurationException;

import static java.time.ZoneOffset.UTC;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.enums.MediaRequestStatus.OPEN;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;

@SuppressWarnings({"PMD.AvoidThrowingRawExceptionTypes"})
@ExtendWith(MockitoExtension.class)
class OutboundFileZipGeneratorImplTest {

    // Any value that exceeds the size of the read buffer of the tested impl
    private static final int DUMMY_FILE_SIZE = 2048;
    private static final Instant SOME_INSTANT = Instant.now();
    private static final Instant SOME_START_TIME = SOME_INSTANT.minus(45, ChronoUnit.MINUTES);
    private static final Instant SOME_END_TIME = SOME_INSTANT.minus(15, ChronoUnit.MINUTES);

    private OutboundFileZipGeneratorImpl outboundFileZipGenerator;
    private Path tempDirectory;

    @Mock
    private AudioConfigurationProperties audioConfigurationProperties;

    @Mock
    private EventRepository eventRepository;

    @BeforeEach
    void setUp() throws IOException, ParserConfigurationException {
        OutboundFileZipGeneratorHelper outboundFileZipGeneratorHelper = new OutboundFileZipGeneratorHelperImpl(
            new AnnotationXmlGeneratorImpl(), eventRepository
        );

        outboundFileZipGenerator = new OutboundFileZipGeneratorImpl(
            audioConfigurationProperties,
            outboundFileZipGeneratorHelper
        );

        var tempDirectoryName = UUID.randomUUID().toString();
        tempDirectory = Files.createTempDirectory(tempDirectoryName);

        when(audioConfigurationProperties.getTempBlobWorkspace())
            .thenReturn(tempDirectory.toString());
    }

    @Test
    void generateAndWriteZipShouldProduceZipWithTheExpectedFileStructure() {
        var audioWithSession1AndChannel1 = createDummyFileAndAudioFileInfo(1);
        var audioWithSession1AndChannel2 = createDummyFileAndAudioFileInfo(2);
        List<AudioFileInfo> session1 = List.of(
            audioWithSession1AndChannel1,
            audioWithSession1AndChannel2
        );

        var audioWithSession2AndChannel1 = createDummyFileAndAudioFileInfo(1);
        List<AudioFileInfo> session2 = List.of(
            audioWithSession2AndChannel1
        );

        var caseNumber = "T20190024";
        Path path = outboundFileZipGenerator.generateAndWriteZip(
            List.of(session1, session2),
            createDummyMediaRequestEntity(caseNumber)
        );

        assertTrue(Files.exists(path));

        List<String> paths = readZipStructure(path);

        assertEquals(7, paths.size());
        assertThat(paths, hasItem("readMe.txt"));
        assertThat(paths, hasItem("playlist.xml"));
        assertThat(paths, hasItem("daudio/localaudio/T2019/0024/0001/0001.a00"));
        assertThat(paths, hasItem("daudio/localaudio/T2019/0024/0001/0001.a01"));
        assertThat(paths, hasItem("daudio/localaudio/T2019/0024/0001/annotations.xml"));
        assertThat(paths, hasItem("daudio/localaudio/T2019/0024/0002/0002.a00"));
        assertThat(paths, hasItem("daudio/localaudio/T2019/0024/0002/annotations.xml"));
    }

    private MediaRequestEntity createDummyMediaRequestEntity(String caseNumber) {

        HearingEntity mockHearingEntity = mock(HearingEntity.class);
        CourtCaseEntity mockCourtCaseEntity = mock(CourtCaseEntity.class);
        CourtroomEntity mockCourtroomEntity = mock(CourtroomEntity.class);
        when(mockHearingEntity.getCourtroom()).thenReturn(mockCourtroomEntity);
        when(mockHearingEntity.getCourtCase()).thenReturn(mockCourtCaseEntity);

        EventEntity eventEntity = new EventEntity();
        eventEntity.setCourtroom(mockCourtroomEntity);
        EventHandlerEntity eventType = mock(EventHandlerEntity.class);
        eventEntity.setEventType(eventType);
        eventEntity.setEventId(1);
        eventEntity.setEventText("Start Event");
        OffsetDateTime utcStartTime = OffsetDateTime.ofInstant(SOME_START_TIME, UTC);
        eventEntity.setTimestamp(utcStartTime.plusMinutes(5));

        when(eventRepository.findAllByHearingId(anyInt())).thenReturn(List.of(eventEntity));
        CourthouseEntity mockCourthouseEntity = mock(CourthouseEntity.class);
        when(mockCourtroomEntity.getCourthouse()).thenReturn(mockCourthouseEntity);
        when(mockCourthouseEntity.getDisplayName()).thenReturn("SWANSEA");
        when(mockCourtCaseEntity.getCaseNumber()).thenReturn(caseNumber);

        UserAccountEntity mockUserAccountEntity = mock(UserAccountEntity.class);

        MediaRequestEntity mediaRequestEntity = new MediaRequestEntity();
        mediaRequestEntity.setId(2023);
        mediaRequestEntity.setHearing(mockHearingEntity);
        mediaRequestEntity.setStartTime(utcStartTime);
        OffsetDateTime utcEndTime = OffsetDateTime.ofInstant(SOME_END_TIME, UTC);
        mediaRequestEntity.setEndTime(utcEndTime);
        mediaRequestEntity.setRequestor(mockUserAccountEntity);
        mediaRequestEntity.setStatus(OPEN);
        mediaRequestEntity.setRequestType(DOWNLOAD);
        mediaRequestEntity.setAttempts(0);
        OffsetDateTime utcNow = OffsetDateTime.ofInstant(SOME_INSTANT, UTC);
        mediaRequestEntity.setCreatedDateTime(utcNow);
        mediaRequestEntity.setCreatedBy(mockUserAccountEntity);
        mediaRequestEntity.setLastModifiedDateTime(utcNow);
        mediaRequestEntity.setLastModifiedBy(mockUserAccountEntity);
        return mediaRequestEntity;
    }

    private AudioFileInfo createDummyFileAndAudioFileInfo(int channel) {
        Path path = createDummyFile();
        return AudioFileInfo.builder()
            .startTime(SOME_START_TIME)
            .endTime(SOME_END_TIME)
            .channel(channel)
            .mediaFile(path.getFileName().toString())
            .path(path)
            .build();
    }

    private Path createDummyFile() {
        var tempFilename = UUID.randomUUID().toString();
        try {
            return Files.write(tempDirectory.resolve(tempFilename), new byte[DUMMY_FILE_SIZE]);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("PMD.AssignmentInOperand")
    private List<String> readZipStructure(Path path) {
        List<String> paths = new ArrayList<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(path))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                paths.add(entry.getName());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return paths;
    }

}
