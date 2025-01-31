package uk.gov.hmcts.darts.audio.helper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.audio.entity.MediaRequestEntity;
import uk.gov.hmcts.darts.audio.enums.AudioRequestOutputFormat;
import uk.gov.hmcts.darts.audio.model.AudioFileInfo;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.entity.TransformedMediaEntity;
import uk.gov.hmcts.darts.common.entity.TransientObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.repository.TransformedMediaRepository;
import uk.gov.hmcts.darts.common.repository.UserAccountRepository;
import uk.gov.hmcts.darts.common.service.TransientObjectDirectoryService;
import uk.gov.hmcts.darts.common.util.DateConverterUtil;
import uk.gov.hmcts.darts.datamanagement.api.DataManagementApi;
import uk.gov.hmcts.darts.datamanagement.model.BlobClientUploadResponse;
import uk.gov.hmcts.darts.notification.api.NotificationApi;
import uk.gov.hmcts.darts.notification.dto.SaveNotificationToDbRequest;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.audiorequests.model.AudioRequestType.DOWNLOAD;
import static uk.gov.hmcts.darts.datamanagement.DataManagementConstants.MetaDataNames.MEDIA_REQUEST_ID;
import static uk.gov.hmcts.darts.datamanagement.DataManagementConstants.MetaDataNames.TRANSFORMED_MEDIA_ID;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_END_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.AUDIO_START_TIME;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.COURTHOUSE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.DEFENDANTS;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.HEARING_DATE;
import static uk.gov.hmcts.darts.notification.NotificationConstants.ParameterMapValues.REQUEST_ID;

@Component
@Slf4j
@RequiredArgsConstructor
public class TransformedMediaHelper {
    private final TransientObjectDirectoryService transientObjectDirectoryService;
    private final TransformedMediaRepository transformedMediaRepository;
    private final DataManagementApi dataManagementApi;
    private final NotificationApi notificationApi;
    private final UserAccountRepository userAccountRepository;

    private static final String NO_DEFENDANTS = "There are no defendants for this hearing";
    private static final String NOT_AVAILABLE = "N/A";

    @Transactional
    public UUID saveToStorage(MediaRequestEntity mediaRequest, InputStream inputStream, String filename, AudioFileInfo audioFileInfo) {
        //save in outbound datastore
        Map<String, String> metadata = new HashMap<>();
        metadata.put(MEDIA_REQUEST_ID, String.valueOf(mediaRequest.getId()));
        BlobClientUploadResponse blobClientUploadResponse = dataManagementApi.saveBlobToContainer(inputStream, DatastoreContainerType.OUTBOUND, metadata);
        final UUID blobName = blobClientUploadResponse.getBlobName();

        OffsetDateTime startTime = audioFileInfo.getStartTime().atOffset(ZoneOffset.UTC);
        OffsetDateTime endTime = audioFileInfo.getEndTime().atOffset(ZoneOffset.UTC);
        //save in database
        TransformedMediaEntity transformedMediaEntity = createTransformedMediaEntity(mediaRequest, filename, startTime, endTime,
                                                                                     blobClientUploadResponse.getBlobSize());
        TransientObjectDirectoryEntity transientObjectDirectoryEntity = transientObjectDirectoryService.saveTransientObjectDirectoryEntity(
            transformedMediaEntity,
            blobName
        );
        Map<String, String> retrospectiveMetadata = Map.of(TRANSFORMED_MEDIA_ID, String.valueOf(transientObjectDirectoryEntity.getTransformedMedia().getId()));
        blobClientUploadResponse.addMetadata(retrospectiveMetadata);

        return blobName;
    }

    @Transactional
    public TransformedMediaEntity createTransformedMediaEntity(MediaRequestEntity mediaRequest, String filename,
                                                               OffsetDateTime startTime, OffsetDateTime endTime,
                                                               Long fileSize) {
        AudioRequestOutputFormat audioRequestOutputFormat = AudioRequestOutputFormat.MP3;
        if (mediaRequest.getRequestType().equals(DOWNLOAD)) {
            audioRequestOutputFormat = AudioRequestOutputFormat.ZIP;
        }
        TransformedMediaEntity entity = new TransformedMediaEntity();
        entity.setMediaRequest(mediaRequest);
        entity.setOutputFilename(filename);
        entity.setStartTime(startTime);
        entity.setEndTime(endTime);
        //By manually setting these values it bypasses the auto update of the last modified by and created by fields
        entity.setLastModifiedBy(mediaRequest.getCreatedBy());
        entity.setCreatedBy(mediaRequest.getCreatedBy());
        entity.setOutputFormat(audioRequestOutputFormat);
        if (nonNull(fileSize)) {
            entity.setOutputFilesize(fileSize.intValue());
        }
        return transformedMediaRepository.save(entity);
    }

    @SuppressWarnings({"PMD.CognitiveComplexity"})
    public void notifyUser(MediaRequestEntity mediaRequestEntity,
                           CourtCaseEntity courtCase,
                           String notificationTemplateName) {
        log.info("Scheduling notification for template name {}, request id {} and court case id {}", notificationTemplateName, mediaRequestEntity.getId(),
                 courtCase.getId()
        );

        Optional<UserAccountEntity> userAccount = userAccountRepository.findById(mediaRequestEntity.getRequestor().getId());

        if (userAccount.isPresent()) {
            Map<String, String> templateParams = new HashMap<>();

            if (notificationTemplateName.equals(NotificationApi.NotificationTemplate.ERROR_PROCESSING_AUDIO.toString())) {

                String defendants = String.join(", ", mediaRequestEntity.getHearing().getCourtCase().getDefendantStringList());

                if (StringUtils.isBlank(defendants)) {
                    defendants = NO_DEFENDANTS;
                }

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

                String courthouseName = mediaRequestEntity.getHearing().getCourtCase().getCourthouse().getDisplayName() != null
                    ? mediaRequestEntity.getHearing().getCourtCase().getCourthouse().getDisplayName() : NOT_AVAILABLE;

                String hearingDate = getFormattedHearingDate(mediaRequestEntity.getHearing().getHearingDate());

                String audioStartTime = mediaRequestEntity.getStartTime() != null
                    ? DateConverterUtil.toLocalDateTime(mediaRequestEntity.getStartTime()).format(formatter) : NOT_AVAILABLE;

                String audioEndTime = mediaRequestEntity.getEndTime() != null
                    ? DateConverterUtil.toLocalDateTime(mediaRequestEntity.getEndTime()).format(formatter) : NOT_AVAILABLE;

                templateParams.put(REQUEST_ID, String.valueOf(mediaRequestEntity.getId()));
                templateParams.put(COURTHOUSE, courthouseName);
                templateParams.put(DEFENDANTS, defendants);
                templateParams.put(HEARING_DATE, hearingDate);
                templateParams.put(AUDIO_START_TIME, audioStartTime);
                templateParams.put(AUDIO_END_TIME, audioEndTime);
            }

            var saveNotificationToDbRequest = SaveNotificationToDbRequest.builder()
                .eventId(notificationTemplateName)
                .caseId(courtCase.getId())
                .userAccountsToEmail(List.of(userAccount.get()))
                .templateValues(templateParams)
                .build();

            notificationApi.scheduleNotification(saveNotificationToDbRequest);

            log.debug("Notification scheduled successfully for request id {} and court case {}", mediaRequestEntity.getId(), courtCase.getId());
        } else {
            log.error("No notification scheduled for request id {} and court case {} ", mediaRequestEntity.getId(), courtCase.getId());
        }
    }

    private static String getFormattedHearingDate(LocalDate dateOfHearing) {

        if (dateOfHearing != null) {
            int day = dateOfHearing.getDayOfMonth();
            Month month = dateOfHearing.getMonth();
            int year = dateOfHearing.getYear();

            String strMonth = Pattern.compile("^.").matcher(month.toString().toLowerCase(Locale.UK)).replaceFirst(m -> m.group().toUpperCase(Locale.UK));
            return day + getNthNumber(day) + " " + strMonth + " " + year;
        } else {
            return NOT_AVAILABLE;
        }
    }

    public static String getNthNumber(int day) {
        if (day > 3 && day < 21) {
            return "th";
        }

        return switch (day % 10) {
            case 1 -> "st";
            case 2 -> "nd";
            case 3 -> "rd";
            default -> "th";
        };
    }

}
