package uk.gov.hmcts.darts.task.runner.dailylist.mapper;

import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.task.runner.dailylist.schemas.courtservice.DailyListStructure;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListRequest;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.DateUtil;
import uk.gov.hmcts.darts.task.runner.dailylist.utilities.deserializer.LocalDateTypeDeserializer;

import java.time.LocalDate;
import javax.xml.datatype.XMLGregorianCalendar;

@UtilityClass
public class DailyListXmlRequestMapper {
    public PostDailyListRequest mapToPostDailyListRequest(DailyListStructure legacyDailyListObject, String document, String sourceSystem, String messageId) {
        PostDailyListRequest request = new PostDailyListRequest();
        request.setSourceSystem(sourceSystem);
        request.setCourthouse(legacyDailyListObject.getCrownCourt().getCourtHouseName());
        request.setUniqueId(legacyDailyListObject.getDocumentID().getUniqueID());
        request.setMessageId(messageId);
        XMLGregorianCalendar publishedTime = legacyDailyListObject.getDocumentID().getTimeStamp();//changed to DocumentId.timestamp as part of DMP-3086
        request.setPublishedTs(DateUtil.toOffsetDateTime(publishedTime));
        XMLGregorianCalendar hearingDateGregCal = legacyDailyListObject.getListHeader().getStartDate();
        LocalDate hearingLocalDate = LocalDateTypeDeserializer.getLocalDate(hearingDateGregCal.toString());
        request.setHearingDate(hearingLocalDate);
        request.setXmlDocument(removeLineBreaks(document));
        return request;
    }

    public String removeLineBreaks(String dailyListXml) {
        return dailyListXml.replaceAll("\\r\\n|\\r|\\n", "");
    }
}
