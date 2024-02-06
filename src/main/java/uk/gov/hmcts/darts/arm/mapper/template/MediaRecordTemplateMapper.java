package uk.gov.hmcts.darts.arm.mapper.template;

import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordCoreProperties.CONTRIBUTOR_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordCoreProperties.DZ_FILE_NAME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordCoreProperties.FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordCoreProperties.FILE_TAG_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CASE_NUMBERS_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CHANNEL_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CHECKSUM_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.COMMENTS_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.COURTHOUSE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.COURTROOM_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.CREATED_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.END_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.FILE_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.HEARING_DATE_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.MAX_CHANNELS_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.OBJECT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.PARENT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.PropertyConstants.ArchiveRecordPropertyValues.START_DATE_TIME_KEY;


public class MediaRecordTemplateMapper extends BaseTemplate {


    public static final String CONTRIBUTOR_SEPARATOR = " & ";

    public MediaRecordTemplateMapper(ArmDataManagementConfiguration armDataManagementConfiguration,
                                     CurrentTimeHelper currentTimeHelper) {
        super(armDataManagementConfiguration, currentTimeHelper);
    }

    public String mapTemplateContents(ExternalObjectDirectoryEntity externalObjectDirectory, String templateFileContents) {
        String contents = super.mapTemplateContents(externalObjectDirectory, templateFileContents);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-DD");
        MediaEntity media = externalObjectDirectory.getMedia();

        contents = parseNullableDateTime(media.getCreatedDateTime(), dateTimeFormatter, contents, CREATED_DATE_TIME_KEY);

        String caseNumbers = null;
        if (nonNull(media.getCaseNumberList())) {
            caseNumbers = caseListToString(media.getCaseNumberList());
        }
        contents = parseNullableStrings(caseNumbers, contents, CASE_NUMBERS_KEY);

        contents = parseNullableStrings(media.getChecksum(), contents, CHECKSUM_KEY);

        String hearingDate = null;
        if (!CollectionUtils.isEmpty(media.getHearingList())) {
            hearingDate = media.getHearingList().get(0).getHearingDate().format(dateFormatter);
        }
        contents = parseNullableStrings(hearingDate, contents, HEARING_DATE_KEY);

        String comments = "";
        String courthouseName = media.getCourtroom().getCourthouse().getCourthouseName();
        String courtroomName = media.getCourtroom().getName();

        return contents.replaceAll(FILENAME_KEY, media.getMediaFile())
            .replaceAll(CONTRIBUTOR_KEY, courthouseName + CONTRIBUTOR_SEPARATOR + courtroomName)
            .replaceAll(FILE_TYPE_KEY, media.getMediaFormat())
            .replaceAll(COMMENTS_KEY, comments)
            .replaceAll(OBJECT_ID_KEY, String.valueOf(media.getId()))
            .replaceAll(PARENT_ID_KEY, String.valueOf(media.getId()))
            .replaceAll(CHANNEL_KEY, String.valueOf(media.getChannel()))
            .replaceAll(MAX_CHANNELS_KEY, String.valueOf(media.getTotalChannels()))
            .replaceAll(START_DATE_TIME_KEY, media.getStart().format(dateTimeFormatter))
            .replaceAll(END_DATE_TIME_KEY, media.getEnd().format(dateTimeFormatter))
            .replaceAll(COURTHOUSE_KEY, courthouseName)
            .replaceAll(COURTROOM_KEY, courtroomName)
            .replaceAll(DZ_FILE_NAME_KEY, media.getMediaFile())
            .replaceAll(FILE_TAG_KEY, media.getMediaFormat());
    }

    private String parseNullableStrings(String parseableValue, String contents, String key) {
        if (nonNull(parseableValue)) {
            contents = contents.replaceAll(key, parseableValue);
        } else {
            while (contents.contains(key)) {
                contents = contents.replace(key, "");
            }
        }
        return contents;
    }

    private static String parseNullableDateTime(OffsetDateTime offsetDateTime, DateTimeFormatter dateTimeFormatter, String contents, String key) {
        if (nonNull(offsetDateTime)) {
            String createdDateTime = offsetDateTime.format(dateTimeFormatter);
            contents = contents.replaceAll(key, createdDateTime);
        } else {
            while (contents.contains(key)) {
                contents = contents.replace(key, "");
            }
        }
        return contents;
    }

}
