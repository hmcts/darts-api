package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.component.impl.AnnotationXmlGeneratorImpl;
import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;
import uk.gov.hmcts.darts.audio.model.xml.Playlist;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.common.util.CommonTestDataUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.COURTHOUSE_README_LABEL;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.END_TIME_README_LABEL;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.RAISED_BY_README_LABEL;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.README_TXT_FILENAME;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.REQUEST_TYPE_README_LABEL;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.START_TIME_README_LABEL;
import static uk.gov.hmcts.darts.common.util.TestUtils.readTempFileContent;
import static uk.gov.hmcts.darts.common.util.TestUtils.unmarshalXmlFile;


@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.AssignmentInOperand", "PMD.ExcessiveImports"})
@Slf4j
@ExtendWith(MockitoExtension.class)
class ViqHeaderServiceImplTest {

    private static final String CASE_NUMBER = "T2023041301_1";
    private static final String EVENT_TIMESTAMP = "2023-07-01T10:00:00";

    @InjectMocks
    ViqHeaderServiceImpl viqHeaderService;

    @Mock
    HearingRepository hearingRepository;

    @Mock
    AnnotationXmlGeneratorImpl annotationXmlGenerator;

    @TempDir
    File tempDirectory;


    @Test
    void generatePlaylistReturnsXmlFile() throws IOException, JAXBException {
        Set<PlaylistInfo> playlistInfos = new LinkedHashSet<>();
        playlistInfos.add(createPlaylistInfo1());
        playlistInfos.add(createPlaylistInfo2());

        String playlistOutputFile = tempDirectory.getAbsolutePath();

        String playListFile = viqHeaderService.generatePlaylist(playlistInfos, playlistOutputFile);
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
        assertEquals("12", playlist.getItems().get(0).getStartTimeHour());
        assertEquals("0", playlist.getItems().get(0).getStartTimeMinutes());
        assertEquals("0", playlist.getItems().get(0).getStartTimeSeconds());

    }

    @Test
    void generatePlayListWithNullPlayListInfoThrowsException() throws IOException {
        String playlistOutputFile = tempDirectory.getAbsolutePath();
        assertThrows(DartsApiException.class, () -> viqHeaderService.generatePlaylist(null, playlistOutputFile));
    }

    @Test
    void generatePlayListWithNullPlayListInfoAndNullPathThrowsException() {
        assertThrows(DartsApiException.class, () -> viqHeaderService.generatePlaylist(null, null));
    }

    @Test
    void generateAnnotationReturnsXmlFile() throws JAXBException, IOException {

        List<HearingEntity> hearingEntities = createHearingInfo();
        OffsetDateTime startTime = CommonTestDataUtil.createOffsetDateTime("2023-07-01T09:00:00");
        OffsetDateTime endTime = CommonTestDataUtil.createOffsetDateTime("2023-07-01T12:00:00");
        String annotationsOutputFile = tempDirectory.getAbsolutePath();

        when(hearingRepository.findAllById(Collections.singleton(hearingEntities.get(0).getId()))).thenReturn(
            hearingEntities);

        String annotationsFile = viqHeaderService.generateAnnotation(
            hearingEntities.get(0).getId(),
            startTime,
            endTime,
            annotationsOutputFile
        );

        assertTrue(Files.exists(Path.of(annotationsFile)));

        String xmlContent = readTempFileContent(annotationsFile);
        assertTrue(xmlContent.contains("D=\"1\""));
        assertTrue(xmlContent.contains("H=\"10\""));
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
    void generateAnnotationWithEventOutsideRequestTimeReturnsXmlFile() throws JAXBException, IOException {

        List<HearingEntity> hearingEntities = createHearingInfo();
        OffsetDateTime startTime = CommonTestDataUtil.createOffsetDateTime("2023-07-01T09:00:00");
        OffsetDateTime endTime = CommonTestDataUtil.createOffsetDateTime("2023-07-01T09:59:59");
        String annotationsOutputFile = tempDirectory.getAbsolutePath();

        when(hearingRepository.findAllById(Collections.singleton(hearingEntities.get(0).getId()))).thenReturn(
            hearingEntities);

        String annotationsFile = viqHeaderService.generateAnnotation(
            hearingEntities.get(0).getId(),
            startTime,
            endTime,
            annotationsOutputFile
        );

        assertTrue(Files.exists(Path.of(annotationsFile)));

        String xmlContent = readTempFileContent(annotationsFile);
        assertTrue(xmlContent.contains("count=\"0\""));
    }

    @Test
    void testBuildAnnotationsDocumentWithInvalidPath() {

        List<HearingEntity> hearingEntities = createHearingInfo();
        OffsetDateTime startTime = CommonTestDataUtil.createOffsetDateTime("2023-07-01T09:00:00");
        OffsetDateTime endTime = CommonTestDataUtil.createOffsetDateTime("2023-07-01T09:59:59");
        String invalidPath = "/non_existent_directory/";

        when(hearingRepository.findAllById(Collections.singleton(hearingEntities.get(0).getId()))).thenReturn(
            hearingEntities);

        assertThrows(RuntimeException.class, () ->
            viqHeaderService.generateAnnotation(hearingEntities.get(0).getId(), startTime, endTime, invalidPath));
    }

    @Test
    void generateReadmeCreatesReadmeWithContent() throws IOException {
        ViqMetaData viqMetaData = ViqMetaData.builder()
            .courthouse("Trainwell Crown Court")
            .raisedBy(null)
            .startTime(Date.from(OffsetDateTime.parse("2023-03-24T09:00:00.000Z").toInstant()))
            .endTime(Date.from(OffsetDateTime.parse("2023-03-24T12:00:00.000Z").toInstant()))
            .type(null)
            .build();

        String fileLocation = tempDirectory.getAbsolutePath();
        String readmeFile = viqHeaderService.generateReadme(viqMetaData, fileLocation);
        assertNotNull(readmeFile);
        log.debug("Reading file " + readmeFile);
        assertTrue(readmeFile.endsWith(README_TXT_FILENAME));
        try (BufferedReader reader = Files.newBufferedReader(Paths.get(readmeFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(COURTHOUSE_README_LABEL)) {
                    assertEquals("Courthouse: Trainwell Crown Court", line);
                } else if (line.startsWith(START_TIME_README_LABEL)) {
                    assertEquals("Start Time: Fri Mar 24 09:00:00 GMT 2023", line);
                } else if (line.startsWith(END_TIME_README_LABEL)) {
                    assertEquals("End Time: Fri Mar 24 12:00:00 GMT 2023", line);
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
            .startTime(OffsetDateTime.parse("2023-06-11T12:00Z"))
            .fileLocation("daudio/localaudio/T2023/041301_1/0001")
            .build();
    }

    private PlaylistInfo createPlaylistInfo2() {
        return PlaylistInfo.builder()
            .caseNumber(CASE_NUMBER)
            .startTime(OffsetDateTime.parse("2023-06-11T13:00Z"))
            .fileLocation("daudio/localaudio/T2023/041301_1/0002")
            .build();
    }


    private List<HearingEntity> createHearingInfo() {
        List<HearingEntity> hearingEntities = new ArrayList<>();
        List<EventEntity> entities = new ArrayList<>();
        HearingEntity hearingEntity = CommonTestDataUtil.createHearing("Case0000001", LocalTime.of(10, 0));
        EventEntity event = CommonTestDataUtil.createEvent("LOG", "Start Recording", hearingEntity,
                                                           CommonTestDataUtil.createOffsetDateTime(EVENT_TIMESTAMP)
        );

        entities.add(event);
        hearingEntity.setEventList(entities);
        hearingEntities.add(hearingEntity);

        return hearingEntities;
    }

}
