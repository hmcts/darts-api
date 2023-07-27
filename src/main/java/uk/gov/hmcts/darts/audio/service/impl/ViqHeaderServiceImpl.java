package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.exception.AudioError;
import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;
import uk.gov.hmcts.darts.audio.model.xml.Playlist;
import uk.gov.hmcts.darts.audio.model.xml.ViqPlayListItem;
import uk.gov.hmcts.darts.audio.service.ViqHeaderService;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.List;

import static java.lang.String.format;

@Slf4j
@Service
public class ViqHeaderServiceImpl implements ViqHeaderService {

    private static final String VIQ_HEADER_SERVICE_NOT_SUPPORTED_MESSAGE = "VIQ Header Service not yet implemented";

    private static final String PLAYLIST_DATE_TIME_ATTRIBUTE = "%d";
    private static final String INVALID_PLAYLIST_INFORMATION = "Invalid playlist information";
    public static final String PLAYLIST_XML_FILENAME = "playlist.xml";

    public static final String README_TXT_FILENAME = "Readme.txt";
    public static final String COURTHOUSE_README_LABEL = "Courthouse";
    public static final String RAISED_BY_README_LABEL = "Raised by";
    public static final String START_TIME_README_LABEL = "Start Time";
    public static final String END_TIME_README_LABEL = "End Time";
    public static final String REQUEST_TYPE_README_LABEL = "Type";
    public static final String README_FORMAT = ": %s";

    @Override
    public String generatePlaylist(List<PlaylistInfo> playlistInfos, String outputFileLocation) {
        String playlistFile;
        Playlist playlist = new Playlist();
        try {
            if (playlistInfos == null) {
                throw new IllegalArgumentException(INVALID_PLAYLIST_INFORMATION);
            } else {
                for (PlaylistInfo playlistInfo : playlistInfos) {
                    playlist.getItems().add(createViqPlaylistItem(playlistInfo));
                }
            }

            playlistFile = writePlaylistFile(playlist, outputFileLocation);
        } catch (JAXBException | IllegalArgumentException exception) {
            log.error("Unable to generate playlist.xml: {}", exception.getMessage());
            throw new DartsApiException(AudioError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }

        return playlistFile;
    }


    @Override
    public String generateAnnotation(Integer hearingId, String startTime, String endTime) {
        throw new NotImplementedException(VIQ_HEADER_SERVICE_NOT_SUPPORTED_MESSAGE);
    }

    @SuppressWarnings({"PMD.AvoidFileStream"})
    @Override
    public String generateReadme(ViqMetaData viqMetaData, String fileLocation) {
        File readmeFile = new File(fileLocation, README_TXT_FILENAME);
        log.debug("Writing readme to {}", readmeFile.getAbsoluteFile());
        try (BufferedWriter fileWriter = Files.newBufferedWriter(readmeFile.toPath());
             PrintWriter printWriter = new PrintWriter(fileWriter);) {

            printWriter.println(format(COURTHOUSE_README_LABEL + README_FORMAT, viqMetaData.getCourthouse()));
            printWriter.println(format(RAISED_BY_README_LABEL + README_FORMAT, viqMetaData.getRaisedBy()));
            printWriter.println(format(START_TIME_README_LABEL + README_FORMAT, viqMetaData.getStartTime()));
            printWriter.println(format(END_TIME_README_LABEL + README_FORMAT, viqMetaData.getEndTime()));
            printWriter.print(format(REQUEST_TYPE_README_LABEL + README_FORMAT, viqMetaData.getType()));
        } catch (IOException exception) {
            log.error("Unable to generate readme.tx file: {}", exception.getMessage());
            throw new DartsApiException(AudioError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }
        return readmeFile.getAbsolutePath();
    }

    private static ViqPlayListItem createViqPlaylistItem(PlaylistInfo playlistInfo) {
        ViqPlayListItem playlistItem = new ViqPlayListItem();

        playlistItem.setValue(playlistInfo.getFileLocation());
        playlistItem.setCaseNumber(playlistInfo.getCaseNumber());

        OffsetDateTime mediaStartTime = playlistInfo.getStartTime();

        playlistItem.setStartTimeInMillis(String.valueOf(mediaStartTime.toInstant().toEpochMilli()));
        playlistItem.setStartTimeYear(format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getYear()));
        playlistItem.setStartTimeMonth(format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getMonthValue()));
        playlistItem.setStartTimeDate(format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getDayOfMonth()));
        playlistItem.setStartTimeHour(format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getHour()));
        playlistItem.setStartTimeMinutes(format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getMinute()));
        playlistItem.setStartTimeSeconds(format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getSecond()));

        return playlistItem;
    }

    private String writePlaylistFile(Playlist playlist, String outputFileLocation) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(Playlist.class);
        Marshaller marshaller = context.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        File playlistFile = new File(outputFileLocation, PLAYLIST_XML_FILENAME);
        marshaller.marshal(playlist, playlistFile);
        return playlistFile.getAbsolutePath();
    }
}
