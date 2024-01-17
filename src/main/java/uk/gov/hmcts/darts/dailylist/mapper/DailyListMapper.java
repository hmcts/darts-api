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
import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequest;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.dailylist.model.DocumentID;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyListMapper {

    private final ObjectMapper objectMapper;

    public DailyListEntity createDailyListEntity(DailyListPostRequest postRequest, String listingCourthouse) {
        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setXmlContent(postRequest.getDailyListXml());
        updateDailyListEntity(postRequest, listingCourthouse, dailyListEntity);

        return dailyListEntity;
    }

    public void updateDailyListEntity(DailyListPostRequest postRequest, String listingCourthouse, DailyListEntity dailyListEntity) {
        dailyListEntity.setListingCourthouse(listingCourthouse);
        dailyListEntity.setSource(postRequest.getSourceSystem());
        dailyListEntity.setXmlContent(postRequest.getDailyListXml());
        updateDailyListEntity(dailyListEntity, postRequest.getDailyListJson(), listingCourthouse);
    }

    public void updateDailyListEntity(DailyListPatchRequest patchRequest, DailyListEntity dailyListEntity) {
        updateDailyListEntity(dailyListEntity, patchRequest.getDailyListJson(), dailyListEntity.getListingCourthouse());
    }

    private void updateDailyListEntity(DailyListEntity dailyListEntity, DailyListJsonObject dailyList, String listingCourthouse) {
        if (dailyList == null) {
            return;
        }
        DocumentID documentId = dailyList.getDocumentId();
        dailyListEntity.setUniqueId(documentId.getUniqueId());
        dailyListEntity.setPublishedTimestamp(dailyList.getListHeader().getPublishedTime());
        dailyListEntity.setStartDate(dailyList.getListHeader().getStartDate());
        dailyListEntity.setEndDate(dailyList.getListHeader().getEndDate());
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setListingCourthouse(listingCourthouse);
        try {
            dailyListEntity.setContent(objectMapper.writeValueAsString(dailyList));
        } catch (JsonProcessingException e) {
            log.error(
                "An Error has occurred trying to save the json for courthouse {} to the database",
                listingCourthouse,
                e
            );
            throw new DartsApiException(DailyListError.INTERNAL_ERROR);
        }
    }
}
