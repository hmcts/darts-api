package uk.gov.hmcts.darts.dailylist.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.log.util.DailyListLogJobReport;

import java.util.List;
import java.util.Map;

import static java.time.LocalDate.now;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.groupingBy;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyListProcessorImpl implements DailyListProcessor {

    private final DailyListRepository dailyListRepository;
    private final DailyListUpdater dailyListUpdater;
    private final LogApi logApi;
    private final ProcessDailyListOnDemandTask processDailyListRunnable;

    @Override
    public void processAllDailyListsWithLock(String listingCourthouse, boolean async) {
        Runnable runnable = () -> {
            if (listingCourthouse == null) {
                processAllDailyLists();
            } else {
                processAllDailyListForListingCourthouse(listingCourthouse);
            }
        };

        if (async) {
            processDailyListRunnable.runAsync(runnable);
        } else {
            processDailyListRunnable.run(runnable);
        }
    }

    @Override
    public void processAllDailyLists() {

        stream(SourceType.values()).forEach(sourceType -> {

            var dailyListsGroupedByCourthouse = dailyListRepository.findByStatusAndStartDateAndSourceOrderByPublishedTimestampDescCreatedDateTimeDesc(
                JobStatusType.NEW,
                now(),
                String.valueOf(sourceType)
            ).stream().collect(groupingBy(DailyListEntity::getListingCourthouse));

            DailyListLogJobReport report = new DailyListLogJobReport(getCountOfDailyList(dailyListsGroupedByCourthouse), sourceType);
            try {
                dailyListsGroupedByCourthouse.forEach((listingCourthouse, dailyLists) -> {
                    processDailyListsForSourceType(dailyLists, report);
                });
            } finally {
                reportStats(report);
            }

        });
    }

    private int getCountOfDailyList(Map<String, List<DailyListEntity>> dailyListMap) {
        int count = 0;
        for (String entity : dailyListMap.keySet()) {
            count = count + dailyListMap.get(entity).size();
        }
        return count;
    }

    private void reportStats(DailyListLogJobReport report) {
        logApi.processedDailyListJob(report);
    }

    @Override
    public void processAllDailyListForListingCourthouse(String listingCourthouse) {
        stream(SourceType.values()).forEach(sourceType -> {
            var dailyLists = dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDescCreatedDateTimeDesc(
                listingCourthouse,
                JobStatusType.NEW,
                now(),
                String.valueOf(sourceType)
            );
            DailyListLogJobReport report = new DailyListLogJobReport(dailyLists.size(), sourceType);

            try {
                processDailyListsForSourceType(dailyLists, report);
            } finally {
                reportStats(report);
            }
        });
    }


    private void processDailyListsForSourceType(List<DailyListEntity> dailyLists, DailyListLogJobReport report) {
        // Daily lists are being ordered descending by date so first item will be the most recent version

        if (!dailyLists.isEmpty()) {
            DailyListEntity dailyListEntity = dailyLists.getFirst();
            try {
                dailyListUpdater.processDailyList(dailyListEntity);

                // report on the daily list result
                report.registerResult(dailyListEntity.getStatus());
            } catch (Exception e) {
                dailyListEntity.setStatus(JobStatusType.FAILED);
                report.registerFailed();
                log.error("Failed to process dailylist for dailylist id: {}", dailyListEntity.getId(), e);
            }

            if (dailyLists.size() > 1) {
                List<DailyListEntity> dailyListsToBeIgnored = dailyLists.subList(1, dailyLists.size());
                for (DailyListEntity dailyList : dailyListsToBeIgnored) {
                    dailyList.setStatus(JobStatusType.IGNORED);
                    report.registerResult(JobStatusType.IGNORED);
                }
            }
            dailyListRepository.saveAll(dailyLists);
        }
    }
}