package uk.gov.hmcts.darts.dailylist.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.exception.DailyListError;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequestInternal;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyListMapper {

    private final ObjectMapper objectMapper;

    public DailyListEntity createDailyListFromJson(DailyListPostRequestInternal postRequest) {
        var dailyListEntity = new DailyListEntity();
        dailyListEntity.setXmlContent(postRequest.getDailyListXml());
        dailyListEntity.setSource(postRequest.getSourceSystem());
        dailyListEntity.setXmlContent(postRequest.getDailyListXml());
        dailyListEntity.setMessageId(postRequest.getMessageId());
        dailyListEntity.setUniqueId(postRequest.getDailyListJson().getDocumentId().getUniqueId());

        mapCommonDailyListDetails(dailyListEntity, postRequest.getDailyListJson());

        return dailyListEntity;
    }

    public void updateDailyListEntity(DailyListEntity dailyListEntity, DailyListJsonObject dailyList) {
        if (dailyList == null) {
            return;
        }
        var documentId = dailyList.getDocumentId();
        dailyListEntity.setUniqueId(documentId.getUniqueId());
        mapCommonDailyListDetails(dailyListEntity, dailyList);
    }

    private void mapCommonDailyListDetails(DailyListEntity dailyListEntity, DailyListJsonObject dailyList) {
        dailyListEntity.setListingCourthouse(dailyList.getCrownCourt().getCourtHouseName());
        dailyListEntity.setPublishedTimestamp(dailyList.getListHeader().getPublishedTime());
        dailyListEntity.setStartDate(dailyList.getListHeader().getStartDate());
        dailyListEntity.setEndDate(dailyList.getListHeader().getEndDate());
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setListingCourthouse(dailyListEntity.getListingCourthouse());

        try {
            dailyListEntity.setContent(objectMapper.writeValueAsString(dailyList));
        } catch (JsonProcessingException e) {
            log.error(
                "An error occurred trying to parse and save the JSON for dal_id={}",
                dailyListEntity.getId(),
                e
            );
            throw new DartsApiException(DailyListError.INTERNAL_ERROR, e);
        }
    }
}
