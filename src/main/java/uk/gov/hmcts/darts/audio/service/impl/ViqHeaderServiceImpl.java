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

import java.io.File;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
public class ViqHeaderServiceImpl implements ViqHeaderService {

    private static final String VIQ_HEADER_SERVICE_NOT_SUPPORTED_MESSAGE = "VIQ Header Service not yet implemented";
    private static final String PLAYLIST_DATE_TIME_ATTRIBUTE = "%d";
    private static final String INVALID_PLAYLIST_INFORMATION = "Invalid playlist information";
    public static final String PLAYLIST_XML_FILENAME = "playlist.xml";

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

    @Override
    public String generateReadme(ViqMetaData viqMetaData) {
        throw new NotImplementedException(VIQ_HEADER_SERVICE_NOT_SUPPORTED_MESSAGE);
    }

    private static ViqPlayListItem createViqPlaylistItem(PlaylistInfo playlistInfo) {
        ViqPlayListItem playlistItem = new ViqPlayListItem();

        playlistItem.setValue(playlistInfo.getFileLocation());
        playlistItem.setCaseNumber(playlistInfo.getCaseNumber());

        OffsetDateTime mediaStartTime = playlistInfo.getStartTime();

        playlistItem.setStartTimeInMillis(String.valueOf(mediaStartTime.toInstant().toEpochMilli()));
        playlistItem.setStartTimeYear(String.format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getYear()));
        playlistItem.setStartTimeMonth(String.format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getMonthValue()));
        playlistItem.setStartTimeDate(String.format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getDayOfMonth()));
        playlistItem.setStartTimeHour(String.format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getHour()));
        playlistItem.setStartTimeMinutes(String.format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getMinute()));
        playlistItem.setStartTimeSeconds(String.format(PLAYLIST_DATE_TIME_ATTRIBUTE, mediaStartTime.getSecond()));

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
