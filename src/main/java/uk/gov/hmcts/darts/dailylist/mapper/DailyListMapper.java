package uk.gov.hmcts.darts.dailylist.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import uk.gov.hmcts.darts.common.entity.CourthouseEntity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.exception.DailyListException;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.dailylist.model.DocumentID;

@Component
@RequiredArgsConstructor
public class DailyListMapper {

    private final ObjectMapper objectMapper;


    public DailyListEntity mapToDailyListEntity(DailyListPostRequest postRequest, CourthouseEntity courthouse) {

        DailyListEntity dailyListEntity = new DailyListEntity();
        dailyListEntity.setVersion((short) 1);
        mapToExistingDailyListEntity(postRequest, courthouse, dailyListEntity);
        return dailyListEntity;
    }

    public void mapToExistingDailyListEntity(DailyListPostRequest postRequest, CourthouseEntity courthouse, DailyListEntity dailyListEntity) {
        uk.gov.hmcts.darts.dailylist.model.DailyList dailyList = postRequest.getDailyList();
        DocumentID documentId = dailyList.getDocumentId();
        dailyListEntity.setCourthouse(courthouse);
        dailyListEntity.setUniqueId(documentId.getUniqueId());
        dailyListEntity.setStatus(String.valueOf(JobStatusType.NEW));
        dailyListEntity.setTimestamp(documentId.getTimeStamp());
        dailyListEntity.setStartDate(dailyList.getListHeader().getStartDate());
        dailyListEntity.setEndDate(dailyList.getListHeader().getEndDate());
        dailyListEntity.setSource(postRequest.getSourceSystem());


        try {
            dailyListEntity.setContent(objectMapper.writeValueAsString(dailyList));
        } catch (JsonProcessingException e) {
            throw new DailyListException(e);
        }
    }
}
