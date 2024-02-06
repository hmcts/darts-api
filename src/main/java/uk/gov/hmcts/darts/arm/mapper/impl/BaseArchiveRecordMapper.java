package uk.gov.hmcts.darts.arm.mapper.impl;

public class BaseArchiveRecordMapper {

    public void mapRecord() {
//        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(armDataManagementConfiguration.getDateTimeFormat());
//        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-DD");
//        MediaEntity media = externalObjectDirectory.getMedia();
//
//        contents = parseNullableDateTime(media.getCreatedDateTime(), dateTimeFormatter, contents, CREATED_DATE_TIME_KEY);
//
//        String caseNumbers = null;
//        if (nonNull(media.getCaseNumberList())) {
//            caseNumbers = caseListToString(media.getCaseNumberList());
//        }
//        contents = parseNullableStrings(caseNumbers, contents, CASE_NUMBERS_KEY);
//
//        contents = parseNullableStrings(media.getChecksum(), contents, CHECKSUM_KEY);
//
//        String hearingDate = null;
//        if (!CollectionUtils.isEmpty(media.getHearingList())) {
//            hearingDate = media.getHearingList().get(0).getHearingDate().format(dateFormatter);
//        }
//        contents = parseNullableStrings(hearingDate, contents, HEARING_DATE_KEY);
//
//        String comments = "";
//        String courthouseName = media.getCourtroom().getCourthouse().getCourthouseName();
//        String courtroomName = media.getCourtroom().getName();
//
//        return contents.replaceAll(FILENAME_KEY, media.getMediaFile())
//            .replaceAll(CONTRIBUTOR_KEY, courthouseName + CONTRIBUTOR_SEPARATOR + courtroomName)
//            .replaceAll(FILE_TYPE_KEY, media.getMediaFormat())
//            .replaceAll(COMMENTS_KEY, comments)
//            .replaceAll(OBJECT_ID_KEY, String.valueOf(media.getId()))
//            .replaceAll(PARENT_ID_KEY, String.valueOf(media.getId()))
//            .replaceAll(CHANNEL_KEY, String.valueOf(media.getChannel()))
//            .replaceAll(MAX_CHANNELS_KEY, String.valueOf(media.getTotalChannels()))
//            .replaceAll(START_DATE_TIME_KEY, media.getStart().format(dateTimeFormatter))
//            .replaceAll(END_DATE_TIME_KEY, media.getEnd().format(dateTimeFormatter))
//            .replaceAll(COURTHOUSE_KEY, courthouseName)
//            .replaceAll(COURTROOM_KEY, courtroomName)
//            .replaceAll(DZ_FILE_NAME_KEY, media.getMediaFile())
//            .replaceAll(FILE_TAG_KEY, media.getMediaFormat());
    }
}
