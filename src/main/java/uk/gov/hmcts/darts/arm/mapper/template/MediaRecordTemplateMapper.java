package uk.gov.hmcts.darts.arm.mapper.template;

import org.springframework.util.CollectionUtils;
import uk.gov.hmcts.darts.arm.config.ArmDataManagementConfiguration;
import uk.gov.hmcts.darts.common.entity.ExternalObjectDirectoryEntity;
import uk.gov.hmcts.darts.common.entity.MediaEntity;
import uk.gov.hmcts.darts.common.helper.CurrentTimeHelper;

import java.time.format.DateTimeFormatter;

import static java.util.Objects.nonNull;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.CASE_NUMBERS_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.CHANNEL_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.CHECKSUM_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.COMMENTS_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.CONTRIBUTOR_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.COURTHOUSE_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.COURTROOM_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.CREATED_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.DZ_FILE_NAME_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.END_DATE_TIME_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.FILENAME_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.FILE_TYPE_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.HEARING_DATE_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.MAX_CHANNELS_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.OBJECT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.PARENT_ID_KEY;
import static uk.gov.hmcts.darts.arm.util.TemplateConstants.START_DATE_TIME_KEY;

public class MediaRecordTemplateMapper extends BaseTemplate {


    public static final String CONTRIBUTOR_SEPARATOR = " & ";

    public MediaRecordTemplateMapper(ArmDataManagementConfiguration armDataManagementConfiguration,
                                     CurrentTimeHelper currentTimeHelper) {
        super(armDataManagementConfiguration, currentTimeHelper);
    }

    public String mapTemplateContents(ExternalObjectDirectoryEntity externalObjectDirectory, String templateFileContents) {
        String contents = super.mapTemplateContents(externalObjectDirectory, templateFileContents);

        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd_MMM_uuuu");
        MediaEntity media = externalObjectDirectory.getMedia();
        String courthouseName = media.getCourtroom().getCourthouse().getCourthouseName();
        String courtroomName = media.getCourtroom().getName();
        String createdDateTime = "";
        if (nonNull(media.getCreatedDateTime())) {
            createdDateTime = media.getCreatedDateTime().format(dateTimeFormatter);
        }

        String caseNumbers = "";
        if (nonNull(media.getCaseNumberList())) {
            caseNumbers = caseListToString(media.getCaseNumberList());
        }

        String checksum = "";
        if (nonNull(media.getChecksum())) {
            checksum = media.getChecksum();
        }
        String hearingDate = "";
        if (!CollectionUtils.isEmpty(media.getHearingList())) {
            hearingDate = media.getHearingList().get(1).getHearingDate().format(dateFormatter);
        }
        String comments = "";

        return contents.replaceAll(FILENAME_KEY, media.getMediaFile())
            .replaceAll(CONTRIBUTOR_KEY, courthouseName + CONTRIBUTOR_SEPARATOR + courtroomName)
            .replaceAll(CASE_NUMBERS_KEY, caseNumbers)
            .replaceAll(FILE_TYPE_KEY, media.getMediaFormat())
            .replaceAll(HEARING_DATE_KEY, hearingDate)
            .replaceAll(CHECKSUM_KEY, checksum)
            .replaceAll(COMMENTS_KEY, comments)
            .replaceAll(CREATED_DATE_TIME_KEY, createdDateTime)
            .replaceAll(OBJECT_ID_KEY, String.valueOf(media.getId()))
            .replaceAll(PARENT_ID_KEY, String.valueOf(media.getId()))
            .replaceAll(CHANNEL_KEY, String.valueOf(media.getChannel()))
            .replaceAll(MAX_CHANNELS_KEY, String.valueOf(media.getTotalChannels()))
            .replaceAll(START_DATE_TIME_KEY, media.getStart().format(dateTimeFormatter))
            .replaceAll(END_DATE_TIME_KEY, media.getEnd().format(dateTimeFormatter))
            .replaceAll(COURTHOUSE_KEY, courthouseName)
            .replaceAll(COURTROOM_KEY, courtroomName)
            .replaceAll(DZ_FILE_NAME_KEY, media.getMediaFile());

    }

}
