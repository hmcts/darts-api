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
import uk.gov.hmcts.darts.audio.model.xml.Playlist;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


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
    void generateReadme() {
        assertThrows(NotImplementedException.class, () ->
            viqHeaderService.generateReadme(null));
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
