package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.exception.DailyListError;
import uk.gov.hmcts.darts.dailylist.mapper.DailyListMapper;
import uk.gov.hmcts.darts.dailylist.model.DailyListJsonObject;
import uk.gov.hmcts.darts.dailylist.model.DailyListPatchRequest;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
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
    private final ObjectMapper objectMapper;

    @Value("${darts.daily-list.housekeeping.days-to-keep:30}")
    private int housekeepingDays;

    @Value("${darts.daily-list.housekeeping.enabled:false}")
    private boolean housekeepingEnabled;

    @Override
    /*
    Retrieve the new Daily List, and store it in the database.
     */
    public PostDailyListResponse saveDailyListToDatabase(DailyListPostRequest postRequest) {
        if (postRequest.getDailyListJson() == null) {
            return saveDailyListXmlToDatabase(postRequest);
        }

        DailyListJsonObject dailyList = postRequest.getDailyListJson();

        String uniqueId = dailyList.getDocumentId().getUniqueId();
        Optional<DailyListEntity> existingRecordOpt = dailyListRepository.findByUniqueId(uniqueId);
        DailyListEntity savedDailyListEntity;
        String listingCourthouse = dailyList.getCrownCourt().getCourtHouseName();
        if (existingRecordOpt.isPresent()) {
            //update the record
            savedDailyListEntity = existingRecordOpt.get();
            dailyListMapper.updateDailyListEntity(postRequest, listingCourthouse, savedDailyListEntity);
            dailyListRepository.saveAndFlush(savedDailyListEntity);
        } else {
            //insert new record
            savedDailyListEntity = dailyListMapper.createDailyListEntity(
                  postRequest,
                  listingCourthouse
            );
            dailyListRepository.saveAndFlush(savedDailyListEntity);
        }
        PostDailyListResponse postDailyListResponse = new PostDailyListResponse();
        postDailyListResponse.setDalId(savedDailyListEntity.getId());
        return postDailyListResponse;
    }

    public PostDailyListResponse saveDailyListXmlToDatabase(DailyListPostRequest postRequest) {
        Optional<DailyListEntity> existingRecordOpt = dailyListRepository.findByUniqueId(postRequest.getUniqueId());
        DailyListEntity dailyListEntity;
        dailyListEntity = existingRecordOpt.orElseGet(DailyListEntity::new);

        dailyListEntity.setListingCourthouse(postRequest.getCourthouse());
        dailyListEntity.setXmlContent(postRequest.getDailyListXml());
        dailyListEntity.setSource(postRequest.getSourceSystem());
        dailyListEntity.setStatus(JobStatusType.NEW);
        dailyListEntity.setStartDate(postRequest.getHearingDate());
        dailyListEntity.setUniqueId(postRequest.getUniqueId());
        dailyListEntity.setPublishedTimestamp(postRequest.getPublishedDateTime());
        dailyListRepository.saveAndFlush(dailyListEntity);

        PostDailyListResponse postDailyListResponse = new PostDailyListResponse();
        postDailyListResponse.setDalId(dailyListEntity.getId());
        return postDailyListResponse;

    }

    @Override
    public PostDailyListResponse updateDailyListInDatabase(DailyListPatchRequest patchRequest) {
        Optional<DailyListEntity> foundDailyListOpt = dailyListRepository.findById(patchRequest.getDailyListId());
        if (foundDailyListOpt.isEmpty()) {
            throw new DartsApiException(DailyListError.DAILY_LIST_NOT_FOUND);
        }

        DailyListEntity foundDailyList = foundDailyListOpt.get();
        dailyListMapper.updateDailyListEntity(patchRequest, foundDailyList);
        dailyListRepository.saveAndFlush(foundDailyList);
        try {
            foundDailyList.setContent(objectMapper.writeValueAsString(patchRequest.getDailyListJson()));
        } catch (JsonProcessingException e) {
            log.error(
                  "An Error has occurred trying to save the json for id {} to the database",
                  patchRequest.getDailyListId(),
                  e
            );
            throw new DartsApiException(DailyListError.INTERNAL_ERROR);
        }
        dailyListRepository.saveAndFlush(foundDailyList);

        PostDailyListResponse postDailyListResponse = new PostDailyListResponse();
        postDailyListResponse.setDalId(foundDailyList.getId());
        return postDailyListResponse;
    }

    @Override
    @SchedulerLock(name = "DailyListService_Housekeeping",
          lockAtLeastFor = "PT20S", lockAtMostFor = "PT5M")
    @Scheduled(cron = "${darts.daily-list.housekeeping.cron}")
    public void runHouseKeeping() {
        runHouseKeepingNow();
    }

    @Transactional
    @Override
    public void runHouseKeepingNow() {
        if (housekeepingEnabled) {
            LocalDate dateToDeleteBefore = LocalDate.now().minusDays(housekeepingDays);
            log.info("Starting DailyList housekeeping, deleting anything before {}", dateToDeleteBefore);
            List<DailyListEntity> deletedEntities = dailyListRepository.deleteByStartDateBefore(dateToDeleteBefore);
            log.info("Finished DailyList housekeeping. Deleted {} rows.", deletedEntities.size());
        }
    }
}
