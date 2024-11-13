package uk.gov.hmcts.darts.dailylist.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.darts.authorisation.component.UserIdentity;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.entity.UserAccountEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.exception.DailyListError;
import uk.gov.hmcts.darts.dailylist.mapper.DailyListMapper;
import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequestInternal;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequestInternal;
import uk.gov.hmcts.darts.dailylist.model.PostDailyListResponse;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyListServiceImpl implements DailyListService {

    private final DailyListRepository dailyListRepository;
    private final DailyListMapper dailyListMapper;
    private final UserIdentity userIdentity;

    @Value("${darts.daily-list.housekeeping.days-to-keep:30}")
    private int housekeepingDays;

    @Value("${darts.daily-list.housekeeping.enabled:false}")
    private boolean housekeepingEnabled;

    @Override
    /*
    Retrieve the new Daily List, and store it in the database.
     */
    public PostDailyListResponse saveDailyListToDatabase(DailyListPostRequestInternal postRequest) {
        if (postRequest.getDailyListJson() == null) {
            return saveDailyListXml(postRequest);
        }

        var savedDailyListEntity = dailyListMapper.createDailyListFromJson(postRequest);
        var user = userIdentity.getUserAccount();
        savedDailyListEntity.setCreatedBy(user);
        savedDailyListEntity.setLastModifiedBy(user);
        dailyListRepository.saveAndFlush(savedDailyListEntity);

        var postDailyListResponse = new PostDailyListResponse();
        postDailyListResponse.setDalId(savedDailyListEntity.getId());

        return postDailyListResponse;
    }

    private PostDailyListResponse saveDailyListXml(DailyListPostRequestInternal postRequest) {
        var dailyListEntity = new DailyListEntity();

        dailyListEntity.setListingCourthouse(postRequest.getCourthouse());
        dailyListEntity.setXmlContent(postRequest.getDailyListXml());
        dailyListEntity.setSource(postRequest.getSourceSystem());
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setStartDate(postRequest.getHearingDate());
        dailyListEntity.setUniqueId(postRequest.getUniqueId());
        dailyListEntity.setPublishedTimestamp(postRequest.getPublishedDateTime());
        dailyListEntity.setMessageId(postRequest.getMessageId());
        UserAccountEntity user = userIdentity.getUserAccount();
        if (dailyListEntity.getCreatedBy() == null) {
            dailyListEntity.setCreatedBy(user);
        }
        dailyListEntity.setLastModifiedBy(user);
        dailyListRepository.saveAndFlush(dailyListEntity);

        PostDailyListResponse postDailyListResponse = new PostDailyListResponse();
        postDailyListResponse.setDalId(dailyListEntity.getId());
        return postDailyListResponse;

    }

    @Override
    public PostDailyListResponse updateDailyListInDatabase(DailyListPatchRequestInternal patchRequest) {
        Optional<DailyListEntity> foundDailyListOpt = dailyListRepository.findById(patchRequest.getDailyListId());
        if (foundDailyListOpt.isEmpty()) {
            throw new DartsApiException(DailyListError.DAILY_LIST_NOT_FOUND);
        }

        DailyListEntity foundDailyList = foundDailyListOpt.get();
        dailyListMapper.updateDailyListEntity(foundDailyList, patchRequest.getDailyListJson());
        foundDailyList.setLastModifiedBy(userIdentity.getUserAccount());
        dailyListRepository.saveAndFlush(foundDailyList);

        PostDailyListResponse postDailyListResponse = new PostDailyListResponse();
        postDailyListResponse.setDalId(foundDailyList.getId());
        return postDailyListResponse;
    }

    @Transactional
    @Override
    public void runHouseKeeping(Integer batchSize) {
        if (housekeepingEnabled) {
            LocalDate dateToDeleteBefore = LocalDate.now().minusDays(housekeepingDays);
            log.info("Starting DailyList housekeeping, deleting anything before {}", dateToDeleteBefore);
            List<DailyListEntity> deletedEntities = dailyListRepository.deleteByStartDateBefore(dateToDeleteBefore, Limit.of(batchSize));
            log.info("Finished DailyList housekeeping. Deleted {} rows.", deletedEntities.size());
        }
    }
}
