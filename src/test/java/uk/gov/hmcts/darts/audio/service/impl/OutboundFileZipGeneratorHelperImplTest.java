package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.component.impl.AnnotationXmlGeneratorImpl;
import uk.gov.hmcts.darts.audio.component.impl.OutboundFileZipGeneratorHelperImpl;
import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;
import uk.gov.hmcts.darts.audio.model.xml.Playlist;
import uk.gov.hmcts.darts.audio.component.OutboundFileZipGeneratorHelper;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.component.impl.OutboundFileZipGeneratorHelperImpl.COURTHOUSE_README_LABEL;
import static uk.gov.hmcts.darts.audio.component.impl.OutboundFileZipGeneratorHelperImpl.END_TIME_README_LABEL;
import static uk.gov.hmcts.darts.audio.component.impl.OutboundFileZipGeneratorHelperImpl.RAISED_BY_README_LABEL;
import static uk.gov.hmcts.darts.audio.component.impl.OutboundFileZipGeneratorHelperImpl.README_TXT_FILENAME;
import static uk.gov.hmcts.darts.audio.component.impl.OutboundFileZipGeneratorHelperImpl.REQUEST_TYPE_README_LABEL;
import static uk.gov.hmcts.darts.audio.component.impl.OutboundFileZipGeneratorHelperImpl.START_TIME_README_LABEL;
import static uk.gov.hmcts.darts.common.util.DateConverterUtil.EUROPE_LONDON_ZONE;
import static uk.gov.hmcts.darts.common.util.TestUtils.readTempFileContent;
import static uk.gov.hmcts.darts.common.util.TestUtils.unmarshalXmlFile;


@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.AssignmentInOperand", "PMD.ExcessiveImports"})
@Slf4j
@ExtendWith(MockitoExtension.class)
class OutboundFileZipGeneratorHelperImplTest {

    private static final String CASE_NUMBER = "T2023041301_1";

    private OutboundFileZipGeneratorHelper outboundFileZipGeneratorHelper;

    @Mock
    private EventRepository eventRepository;

    @TempDir
    private File tempDirectory;

    @BeforeEach
    void setUp() throws ParserConfigurationException {
        outboundFileZipGeneratorHelper = new OutboundFileZipGeneratorHelperImpl(new AnnotationXmlGeneratorImpl(), eventRepository);
    }

    @Test
    void generatePlaylistReturnsXmlFile() throws IOException, JAXBException {
        Set<PlaylistInfo> playlistInfos = new LinkedHashSet<>();
        playlistInfos.add(createPlaylistInfo1());
        playlistInfos.add(createPlaylistInfo2());

        String playlistOutputFile = tempDirectory.getAbsolutePath();

        String playListFile = outboundFileZipGeneratorHelper.generatePlaylist(playlistInfos, playlistOutputFile);
        log.debug("Playlist file {}", playListFile);
        assertTrue(Files.exists(Path.of(playListFile)));
        Playlist playlist = unmarshalXmlFile(Playlist.class, playListFile);
        assertEquals("1.0", playlist.getPlaylistVersion());
        assertEquals(2, playlist.getItems().size());
        assertEquals(CASE_NUMBER, playlist.getItems().get(0).getCaseNumber());
        assertNotNull(playlist.getItems().get(0).getStartTimeInMillis());
        assertEquals("2023", playlist.getItems().get(0).getStartTimeYear());
        assertEquals("6", playlist.getItems().get(0).getStartTimeMonth());
        assertEquals("11", playlist.getItems().get(0).getStartTimeDate());
        assertEquals("13", playlist.getItems().get(0).getStartTimeHour());
        assertEquals("0", playlist.getItems().get(0).getStartTimeMinutes());
        assertEquals("0", playlist.getItems().get(0).getStartTimeSeconds());
    }

    @Test
    void generatePlayListWithNullPlayListInfoThrowsException() {
        String playlistOutputFile = tempDirectory.getAbsolutePath();
        assertThrows(DartsApiException.class, () -> outboundFileZipGeneratorHelper.generatePlaylist(null, playlistOutputFile));
    }

    @Test
    void generatePlayListWithNullPlayListInfoAndNullPathThrowsException() {
        assertThrows(DartsApiException.class, () -> outboundFileZipGeneratorHelper.generatePlaylist(null, null));
    }

    @Test
    void generateAnnotationReturnsXmlFile() {

        HearingEntity hearingEntity = createHearingInfo();
        ZonedDateTime startTime = ZonedDateTime.ofInstant(
            Instant.parse("2023-07-01T09:00:00Z"),
            EUROPE_LONDON_ZONE
        );
        ZonedDateTime endTime = ZonedDateTime.ofInstant(
            Instant.parse("2023-07-01T12:00:00Z"),
            EUROPE_LONDON_ZONE
        );
        Path annotationsOutputFile = Path.of(tempDirectory.getAbsolutePath(), "0_annotations.xml");

        String annotationsFile = outboundFileZipGeneratorHelper.generateAnnotation(
            hearingEntity,
            startTime,
            endTime,
            annotationsOutputFile.toString()
        );

        assertTrue(Files.exists(Path.of(annotationsFile)));

        String xmlContent = readTempFileContent(annotationsFile);
        assertTrue(xmlContent.contains("D=\"1\""));
        assertTrue(xmlContent.contains("H=\"11\""));
        assertTrue(xmlContent.contains("L=\"operator\""));
        assertTrue(xmlContent.contains("M=\"7\""));
        assertTrue(xmlContent.contains("MIN=\"0\""));
        assertTrue(xmlContent.contains("N=\"Start Recording\""));
        assertTrue(xmlContent.contains("P=\"3600\""));
        assertTrue(xmlContent.contains("R=\"0\""));
        assertTrue(xmlContent.contains("S=\"0\""));
        assertTrue(xmlContent.contains("T=\"1688205600000\""));
        assertTrue(xmlContent.contains("Y=\"2023\""));
    }

    @Test
    void generateAnnotationWithEventOutsideRequestTimeReturnsXmlFile() {

        HearingEntity hearingEntity = createHearingInfo();

        ZonedDateTime startTime = ZonedDateTime.ofInstant(
            Instant.parse("2023-07-01T09:00:00Z"),
            EUROPE_LONDON_ZONE
        );
        ZonedDateTime endTime = ZonedDateTime.ofInstant(
            Instant.parse("2023-07-01T09:59:59Z"),
            EUROPE_LONDON_ZONE
        );
        Path annotationsOutputFile = Path.of(tempDirectory.getAbsolutePath(), "0_annotations.xml");

        String annotationsFile = outboundFileZipGeneratorHelper.generateAnnotation(
            hearingEntity,
            startTime,
            endTime,
            annotationsOutputFile.toString()
        );

        assertTrue(Files.exists(Path.of(annotationsFile)));

        String xmlContent = readTempFileContent(annotationsFile);
        assertTrue(xmlContent.contains("count=\"0\""));
    }

    @Test
    void testBuildAnnotationsDocumentWithInvalidPath() {

        HearingEntity hearingEntity = createHearingInfo();
        ZonedDateTime startTime = ZonedDateTime.ofInstant(
            Instant.parse("2023-07-01T09:00:00Z"),
            EUROPE_LONDON_ZONE
        );
        ZonedDateTime endTime = ZonedDateTime.ofInstant(
            Instant.parse("2023-07-01T09:59:59Z"),
            EUROPE_LONDON_ZONE
        );
        String invalidPath = "/non_existent_directory/0_annotations.xml";

        var exception = assertThrows(DartsApiException.class, () ->
            outboundFileZipGeneratorHelper.generateAnnotation(hearingEntity, startTime, endTime, invalidPath));

        assertEquals("Failed to process audio request", exception.getMessage());
        assertEquals(
            "java.nio.file.NoSuchFileException: /non_existent_directory/0_annotations.xml",
            exception.getCause().toString()
        );
    }

    @Test
    void generateReadmeCreatesReadmeWithContent() throws IOException {

        ZonedDateTime startTime = ZonedDateTime.ofInstant(
            Instant.parse("2023-03-24T09:00:00.000Z"),
            EUROPE_LONDON_ZONE
        );
        ZonedDateTime endTime = ZonedDateTime.ofInstant(
            Instant.parse("2023-03-24T12:00:00.000Z"),
            EUROPE_LONDON_ZONE
        );

        ViqMetaData viqMetaData = ViqMetaData.builder()
            .courthouse("Trainwell Crown Court")
            .raisedBy(null)
            .startTime(startTime)
            .endTime(endTime)
            .type(null)
            .build();

        String fileLocation = tempDirectory.getAbsolutePath();
        String readmeFile = outboundFileZipGeneratorHelper.generateReadme(viqMetaData, fileLocation);

        assertNotNull(readmeFile);
        log.debug("Reading file " + readmeFile);
        assertTrue(readmeFile.endsWith(README_TXT_FILENAME));
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(readmeFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(COURTHOUSE_README_LABEL)) {
                    assertEquals("Courthouse: Trainwell Crown Court", line);
                } else if (line.startsWith(START_TIME_README_LABEL)) {
                    assertEquals("Start Time: 24 March 2023 at 09:00:00 GMT", line);
                } else if (line.startsWith(END_TIME_README_LABEL)) {
                    assertEquals("End Time: 24 March 2023 at 12:00:00 GMT", line);
                } else if (line.startsWith(RAISED_BY_README_LABEL)) {
                    assertEquals("Raised by: ", line);
                } else if (line.startsWith(REQUEST_TYPE_README_LABEL)) {
                    assertEquals("Type: ", line);
                }
            }
        }
    }


    private PlaylistInfo createPlaylistInfo1() {
        return PlaylistInfo.builder()
            .caseNumber(CASE_NUMBER)
            .startTime(ZonedDateTime.ofInstant(
                Instant.parse("2023-06-11T12:00:00Z"),
                EUROPE_LONDON_ZONE
            ))
            .fileLocation("daudio/localaudio/T2023/041301_1/0001")
            .build();
    }

    private PlaylistInfo createPlaylistInfo2() {
        return PlaylistInfo.builder()
            .caseNumber(CASE_NUMBER)
            .startTime(ZonedDateTime.ofInstant(
                Instant.parse("2023-06-11T13:00:00Z"),
                EUROPE_LONDON_ZONE
            ))
            .fileLocation("daudio/localaudio/T2023/041301_1/0002")
            .build();
    }


    private HearingEntity createHearingInfo() {
        List<EventEntity> eventEntities = new ArrayList<>();
        HearingEntity hearingEntity = CommonTestDataUtil.createHearing("Case0000001", LocalTime.of(10, 0));
        EventEntity eventEntity = CommonTestDataUtil.createEventWith("LOG", "Start Recording", hearingEntity,
                                                                     OffsetDateTime.parse("2023-07-01T10:00:00Z")
        );

        eventEntities.add(eventEntity);
        hearingEntity.setEventList(eventEntities);
        when(eventRepository.findAllByHearingId(anyInt())).thenReturn(eventEntities);
        return hearingEntity;
    }

}
