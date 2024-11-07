package uk.gov.hmcts.darts.audio.component.impl;

import jakarta.xml.bind.JAXBException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.component.OutboundDocumentGenerator;
import uk.gov.hmcts.darts.audio.component.OutboundFileZipGeneratorHelper;
import uk.gov.hmcts.darts.audio.exception.AudioApiError;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.audio.model.PlaylistInfo;
import uk.gov.hmcts.darts.audio.model.ViqAnnotationData;
import uk.gov.hmcts.darts.audio.model.ViqHeader;
import uk.gov.hmcts.darts.audio.model.ViqMetaData;
import uk.gov.hmcts.darts.audio.model.xml.Playlist;
import uk.gov.hmcts.darts.audio.model.xml.ViqPlayListItem;
import uk.gov.hmcts.darts.audio.util.XmlUtil;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.EventRepository;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import static java.lang.String.format;
import static java.time.format.FormatStyle.LONG;
import static java.util.Locale.UK;

@Slf4j
@Service
@SuppressWarnings("PMD.ExcessiveImports")
public class OutboundFileZipGeneratorHelperImpl implements OutboundFileZipGeneratorHelper {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofLocalizedDateTime(LONG)
        .withLocale(UK);
    private static final String DATE_TIME_ATTRIBUTE = "%d";
    private static final String INVALID_PLAYLIST_INFORMATION = "Invalid playlist information";
    public static final String PLAYLIST_XML_FILENAME = "playlist.xml";
    public static final String README_TXT_FILENAME = "Readme.txt";
    public static final String COURTHOUSE_README_LABEL = "Courthouse";
    public static final String RAISED_BY_README_LABEL = "Raised by (User Id)";
    public static final String START_TIME_README_LABEL = "Start Time";
    public static final String END_TIME_README_LABEL = "End Time";
    public static final String REQUEST_TYPE_README_LABEL = "Type";
    public static final String README_FORMAT = ": %s";

    private final OutboundDocumentGenerator annotationXmlGenerator;
    private final EventRepository eventRepository;

    public OutboundFileZipGeneratorHelperImpl(@Qualifier("annotationXmlGenerator") OutboundDocumentGenerator annotationXmlGenerator,
                                              EventRepository eventRepository) {
        this.annotationXmlGenerator = annotationXmlGenerator;
        this.eventRepository = eventRepository;
    }

    @Override
    @SuppressWarnings({"PMD.ExceptionAsFlowControl"})
    public String generatePlaylist(Set<PlaylistInfo> playlistInfos, String outputFileLocation) {
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

            playlistFile = XmlUtil.marshalToXmlFile(
                playlist,
                Playlist.class,
                outputFileLocation,
                PLAYLIST_XML_FILENAME
            );
        } catch (JAXBException | IllegalArgumentException exception) {
            log.error("Unable to generate playlist.xml: {}", exception.getMessage());
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }

        return playlistFile;
    }

    @Override
    public String generateAnnotation(HearingEntity hearingEntity, ZonedDateTime startTime, ZonedDateTime endTime,
                                     String annotationsOutputFile) {
        List<EventEntity> events = getHearingEventsByStartAndEndTime(hearingEntity, startTime, endTime);

        ViqAnnotationData annotationData = ViqAnnotationData.builder()
            .annotationsStartTime(startTime)
            .events(events)
            .build();

        try {
            return annotationXmlGenerator.generateAndWriteXmlFile(annotationData, Path.of(annotationsOutputFile))
                .toString();
        } catch (IOException | TransformerException | ParserConfigurationException exception) {
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }
    }

    @SuppressWarnings({"PMD.AvoidFileStream"})
    @Override
    public String generateReadme(ViqMetaData viqMetaData, String fileLocation) {
        File readmeFile = new File(fileLocation, README_TXT_FILENAME);
        log.debug("Writing readme to {}", readmeFile.getAbsoluteFile());

        try (BufferedWriter fileWriter = Files.newBufferedWriter(readmeFile.toPath());
            PrintWriter printWriter = new PrintWriter(fileWriter)) {

            printWriter.println(format(COURTHOUSE_README_LABEL + README_FORMAT, viqMetaData.getCourthouse()));
            printWriter.println(format(
                RAISED_BY_README_LABEL + README_FORMAT,
                StringUtils.defaultIfEmpty(viqMetaData.getRaisedBy(), "")
            ));
            printWriter.println(format(
                START_TIME_README_LABEL + README_FORMAT,
                viqMetaData.getStartTime().format(DATE_TIME_FORMATTER)
            ));
            printWriter.println(format(
                END_TIME_README_LABEL + README_FORMAT,
                viqMetaData.getEndTime().format(DATE_TIME_FORMATTER)
            ));
            printWriter.print(format(
                REQUEST_TYPE_README_LABEL + README_FORMAT,
                StringUtils.defaultIfEmpty(viqMetaData.getType(), "")
            ));
        } catch (IOException exception) {
            log.error("Unable to generate readme file: {}", readmeFile.getAbsoluteFile(), exception);
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }
        return readmeFile.getAbsolutePath();
    }

    @Override
    public Path generateViqFile(AudioFileInfo audioFileInfo, Path viqOutputFile) {

        ViqHeader viqHeader = new ViqHeader(audioFileInfo.getStartTime());

        try {
            FileUtils.writeByteArrayToFile(viqOutputFile.toFile(), viqHeader.getViqHeaderBytes());
            FileUtils.writeByteArrayToFile(viqOutputFile.toFile(), Files.readAllBytes(audioFileInfo.getPath()), true);
        } catch (Exception exception) {
            log.error("Unable to generate viq header for file: {}", viqOutputFile, exception);
            throw new DartsApiException(AudioApiError.FAILED_TO_PROCESS_AUDIO_REQUEST, exception);
        }

        return viqOutputFile;
    }

    private static ViqPlayListItem createViqPlaylistItem(PlaylistInfo playlistInfo) {
        ViqPlayListItem playlistItem = new ViqPlayListItem();

        playlistItem.setValue(toPlaylistPathFormat(playlistInfo.getFileLocation()));
        playlistItem.setCaseNumber(playlistInfo.getCaseNumber());

        ZonedDateTime itemStartTime = playlistInfo.getStartTime();

        playlistItem.setStartTimeInMillis(String.valueOf(itemStartTime.toInstant().toEpochMilli()));
        playlistItem.setStartTimeYear(format(DATE_TIME_ATTRIBUTE, itemStartTime.getYear()));
        playlistItem.setStartTimeMonth(format(DATE_TIME_ATTRIBUTE, itemStartTime.getMonthValue()));
        playlistItem.setStartTimeDate(format(DATE_TIME_ATTRIBUTE, itemStartTime.getDayOfMonth()));
        playlistItem.setStartTimeHour(format(DATE_TIME_ATTRIBUTE, itemStartTime.getHour()));
        playlistItem.setStartTimeMinutes(format(DATE_TIME_ATTRIBUTE, itemStartTime.getMinute()));
        playlistItem.setStartTimeSeconds(format(DATE_TIME_ATTRIBUTE, itemStartTime.getSecond()));

        return playlistItem;
    }

    private List<EventEntity> getHearingEventsByStartAndEndTime(
        HearingEntity hearingEntity,
        ZonedDateTime startTime,
        ZonedDateTime endTime) {
        List<EventEntity> hearingEvents = eventRepository.findAllByHearingId(hearingEntity.getId());
        return hearingEvents.stream()
            .filter(eventEntity -> !eventEntity.getTimestamp().isBefore(startTime.toOffsetDateTime()))
            .filter(eventEntity -> !eventEntity.getTimestamp().isAfter(endTime.toOffsetDateTime()))
            .sorted(Comparator.comparing(EventEntity::getTimestamp))
            .collect(Collectors.toList());
    }

    private static String toPlaylistPathFormat(String path) {
        String playlistPath = path.replace("/", "\\");
        return playlistPath + "\\";
    }
}