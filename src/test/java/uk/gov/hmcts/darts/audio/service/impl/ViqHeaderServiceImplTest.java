package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;
import uk.gov.hmcts.darts.audio.model.xml.Playlist;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.COURTHOUSE_README_LABEL;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.END_TIME_README_LABEL;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.RAISED_BY_README_LABEL;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.README_TXT_FILENAME;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.REQUEST_TYPE_README_LABEL;
import static uk.gov.hmcts.darts.audio.service.impl.ViqHeaderServiceImpl.START_TIME_README_LABEL;


@SuppressWarnings({"PMD.AvoidInstantiatingObjectsInLoops", "PMD.AssignmentInOperand"})
@Slf4j
@ExtendWith(MockitoExtension.class)
class ViqHeaderServiceImplTest {

    private static final String CASE_NUMBER = "T2023041301_1";

    @InjectMocks
    ViqHeaderServiceImpl viqHeaderService;

    @TempDir
    File tempDirectory;


    @Test
    void generatePlaylistReturnsXmlFile() throws IOException, JAXBException {
        List<PlaylistInfo> playlistInfos = new ArrayList<>();
        playlistInfos.add(createPlaylistInfo1());
        playlistInfos.add(createPlaylistInfo2());

        String playlistOutputFile = tempDirectory.getAbsolutePath();

        String playListFile = viqHeaderService.generatePlaylist(playlistInfos, playlistOutputFile);
        log.debug("Playlist file {}", playListFile);
        assertTrue(Files.exists(Path.of(playListFile)));
        Playlist playlist = unmarshalPlaylist(playListFile);
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
    void generateAnnotation() {
        assertThrows(NotImplementedException.class, () ->
            viqHeaderService.generateAnnotation(null, null, null));
    }

    @Test
    void generateReadmeCreatesReadmeWithContent() throws IOException {
        String courthouse = "Trainwell Crown Court";
        String startTime = "Fri Mar 24 09:00:00 GMT 2023";
        String endTime = "Fri Mar 24 12:00:00 GMT 2023";
        String raisedBy = "User";
        String requestType = "Download";

        ViqMetaData viqMetaData = ViqMetaData.builder()
            .courthouse(courthouse)
            .startTime(startTime)
            .endTime(endTime)
            .raisedBy(raisedBy)
            .type(requestType)
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
                }  else if (line.startsWith(RAISED_BY_README_LABEL)) {
                    assertEquals("Raised by: User", line);
                } else if (line.startsWith(REQUEST_TYPE_README_LABEL)) {
                    assertEquals("Type: Download", line);
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

    public Playlist unmarshalPlaylist(String xmlFile) throws JAXBException, IOException {
        JAXBContext context = JAXBContext.newInstance(Playlist.class);
        return (Playlist) context.createUnmarshaller()
            .unmarshal(Files.newBufferedReader(Path.of(xmlFile)));
    }
}
