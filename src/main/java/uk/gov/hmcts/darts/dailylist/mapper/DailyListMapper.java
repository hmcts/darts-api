package uk.gov.hmcts.darts.dailylist.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.experimental.UtilityClass;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.dailylist.model.DocumentID;

@UtilityClass
public class DailyListMapper {

    public DailyListEntity mapToDailyListEntity(DailyListPostRequest postRequest, Courthouse courthouse) {

        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setVersion((short) 1);
        mapToExistingDailyListEntity(postRequest, courthouse, dailyListEntity);
        return dailyListEntity;
    }

    public void mapToExistingDailyListEntity(DailyListPostRequest postRequest, Courthouse courthouse, DailyListEntity dailyListEntity) {
        uk.gov.hmcts.darts.dailylist.model.DailyList dailyList = postRequest.getDailyList();
        DocumentID documentId = dailyList.getDocumentId();
        dailyListEntity.setCourthouse(courthouse);
        dailyListEntity.setUniqueId(documentId.getUniqueId());
        dailyListEntity.setStatus(JobStatusType.NEW.name());
        dailyListEntity.setTimestamp(documentId.getTimeStamp());
        dailyListEntity.setStartDate(dailyList.getListHeader().getStartDate());
        dailyListEntity.setEndDate(dailyList.getListHeader().getEndDate());
        dailyListEntity.setSource(postRequest.getSourceSystem());

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            dailyListEntity.setContent(objectMapper.writeValueAsString(dailyList.getCourtLists()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);//todo
        }
    }
}
