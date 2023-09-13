package uk.gov.hmcts.darts.dailylist.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
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


    public DailyListEntity createDailyListEntity(DailyListPostRequest postRequest, CourthouseEntity courthouse) {

        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setXmlContent(postRequest.getDailyListXml());
        updateDailyListEntity(postRequest, courthouse, dailyListEntity);
        return dailyListEntity;
    }

    public void updateDailyListEntity(DailyListEntity dailyListEntity, DailyListJsonObject dailyList, CourthouseEntity courthouse) {
        if (dailyList == null) {
            return;
        }
        DocumentID documentId = dailyList.getDocumentId();
        dailyListEntity.setUniqueId(documentId.getUniqueId());
        dailyListEntity.setPublishedTimestamp(documentId.getTimeStamp());
        dailyListEntity.setStartDate(dailyList.getListHeader().getStartDate());
        dailyListEntity.setEndDate(dailyList.getListHeader().getEndDate());
        dailyListEntity.setStatus(String.valueOf(JobStatusType.NEW));
        try {
            dailyListEntity.setContent(objectMapper.writeValueAsString(dailyList));
        } catch (JsonProcessingException e) {
            log.error(
                "An Error has occurred trying to save the json for courthouse {} to the database",
                courthouse.getCourthouseName(),
                e
            );
            throw new DartsApiException(DailyListError.INTERNAL_ERROR);
        }
    }

    public void updateDailyListEntity(DailyListPostRequest postRequest, CourthouseEntity courthouse,
                                      DailyListEntity dailyListEntity) {
        dailyListEntity.setCourthouse(courthouse);
        dailyListEntity.setSource(postRequest.getSourceSystem());
        dailyListEntity.setXmlContent(postRequest.getDailyListXml());
        updateDailyListEntity(dailyListEntity, postRequest.getDailyListJson(), courthouse);
    }

    public void updateDailyListEntity(DailyListPatchRequest patchRequest,
                                      DailyListEntity dailyListEntity) {
        updateDailyListEntity(dailyListEntity, patchRequest.getDailyListJson(), dailyListEntity.getCourthouse());
    }


}
