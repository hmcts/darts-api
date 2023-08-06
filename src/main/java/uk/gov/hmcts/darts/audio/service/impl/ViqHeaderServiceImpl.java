package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.xml.bind.JAXBException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.exception.AudioError;
import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;
import uk.gov.hmcts.darts.audio.model.xml.AnnotationMeta;
import uk.gov.hmcts.darts.audio.model.xml.Playlist;
import uk.gov.hmcts.darts.audio.model.xml.ViqAnnotationItem;
import uk.gov.hmcts.darts.audio.model.xml.ViqPlayListItem;
import uk.gov.hmcts.darts.audio.service.ViqHeaderService;
import uk.gov.hmcts.darts.audio.util.XmlUtil;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Slf4j
@RequiredArgsConstructor
@Service
public class ViqHeaderServiceImpl implements ViqHeaderService {

    private static final String DATE_TIME_ATTRIBUTE = "%d";
    private static final String INVALID_PLAYLIST_INFORMATION = "Invalid playlist information";
    public static final String PLAYLIST_XML_FILENAME = "playlist.xml";
    public static final String ANNOTATION_XML_FILENAME = "annotations.xml";
    public static final String README_TXT_FILENAME = "Readme.txt";
    public static final String COURTHOUSE_README_LABEL = "Courthouse";
    public static final String RAISED_BY_README_LABEL = "Raised by";
    public static final String START_TIME_README_LABEL = "Start Time";
    public static final String END_TIME_README_LABEL = "End Time";
    public static final String REQUEST_TYPE_README_LABEL = "Type";
    public static final String README_FORMAT = ": %s";

    private final HearingRepository hearingRepository;

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

            playlistFile = XmlUtil.marshalToXmlFile(playlist, Playlist.class, outputFileLocation, PLAYLIST_XML_FILENAME);
        } catch (JAXBException | IllegalArgumentException exception) {
            log.error("Unable to generate playlist.xml: {}", exception.getMessage());
            throw new DartsApiException(AudioError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }

        return playlistFile;
    }


    @Override
    public String generateAnnotation(Integer hearingId, OffsetDateTime startTime, OffsetDateTime endTime, String outputFileLocation) {
        List<HearingEntity> hearingEntities = hearingRepository.findAllById(Collections.singleton(hearingId));
        List<EventEntity> events = getHearingEventsByStartAndEndTime(hearingEntities, startTime, endTime);

        AnnotationMeta annotationMeta = new AnnotationMeta();

        try {
            annotationMeta.getAnnotations().setEventCount(String.valueOf(events.size()));

            for (EventEntity event : events) {
                annotationMeta.getAnnotations().getAnnotationItems().add(createViqAnnotationItem(event, startTime));
            }

            return XmlUtil.marshalToXmlFile(annotationMeta, AnnotationMeta.class,
                                        outputFileLocation, ANNOTATION_XML_FILENAME);
        } catch (JAXBException exception) {
            log.error("Unable to generate annotations.xml: {}", exception.getMessage());
            throw new DartsApiException(AudioError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }
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
            log.error("Unable to generate readme.txt file: {}", exception.getMessage());
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
        playlistItem.setStartTimeYear(format(DATE_TIME_ATTRIBUTE, mediaStartTime.getYear()));
        playlistItem.setStartTimeMonth(format(DATE_TIME_ATTRIBUTE, mediaStartTime.getMonthValue()));
        playlistItem.setStartTimeDate(format(DATE_TIME_ATTRIBUTE, mediaStartTime.getDayOfMonth()));
        playlistItem.setStartTimeHour(format(DATE_TIME_ATTRIBUTE, mediaStartTime.getHour()));
        playlistItem.setStartTimeMinutes(format(DATE_TIME_ATTRIBUTE, mediaStartTime.getMinute()));
        playlistItem.setStartTimeSeconds(format(DATE_TIME_ATTRIBUTE, mediaStartTime.getSecond()));

        return playlistItem;
    }

    private ViqAnnotationItem createViqAnnotationItem(EventEntity event, OffsetDateTime startTime) {

        ViqAnnotationItem annotationItem = new ViqAnnotationItem();
        OffsetDateTime eventTimestamp = event.getTimestamp();
        Long lapsed = (eventTimestamp.toInstant().toEpochMilli() - startTime.toInstant().toEpochMilli()) / 1000;

        annotationItem.setEventName(event.getEventName());
        annotationItem.setStartTimeInMillis(String.valueOf(eventTimestamp.toInstant().toEpochMilli()));
        annotationItem.setStartTimeYear(format(DATE_TIME_ATTRIBUTE, eventTimestamp.getYear()));
        annotationItem.setStartTimeMonth(format(DATE_TIME_ATTRIBUTE, eventTimestamp.getMonthValue()));
        annotationItem.setStartTimeDate(format(DATE_TIME_ATTRIBUTE, eventTimestamp.getDayOfMonth()));
        annotationItem.setStartTimeHour(format(DATE_TIME_ATTRIBUTE, eventTimestamp.getHour()));
        annotationItem.setStartTimeMinutes(format(DATE_TIME_ATTRIBUTE, eventTimestamp.getMinute()));
        annotationItem.setStartTimeSeconds(format(DATE_TIME_ATTRIBUTE, eventTimestamp.getSecond()));
        annotationItem.setLapsed(String.valueOf(lapsed));

        return annotationItem;
    }

    private List<EventEntity> getHearingEventsByStartAndEndTime(
        List<HearingEntity> hearingEntities,
        OffsetDateTime startTime,
        OffsetDateTime endTime) {

        return hearingEntities.stream()
            .map(h -> h.getEventList())
            .flatMap(Collection::stream)
            .filter(e -> !e.getTimestamp().isBefore(startTime))
            .filter(e -> !e.getTimestamp().isAfter(endTime))
            .sorted((t1, t2) -> t1.getTimestamp().compareTo(t2.getTimestamp()))
            .collect(Collectors.toList());
    }
}
