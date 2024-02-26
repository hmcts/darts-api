package uk.gov.hmcts.darts.dailylist.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.common.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.enums.JobStatusType;
import uk.gov.hmcts.darts.dailylist.enums.SourceType;
import uk.gov.hmcts.darts.dailylist.service.DailyListProcessor;
import uk.gov.hmcts.darts.log.api.LogApi;
import uk.gov.hmcts.darts.log.util.LogJobReport;

import java.util.List;

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

    private static final String DAILY_LIST_JOB = "Daily list job";

    @Override
    public void processAllDailyLists() {

        stream(SourceType.values()).forEach(sourceType -> {

            var dailyListsGroupedByCourthouse = dailyListRepository.findByStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                JobStatusType.NEW,
                now(),
                String.valueOf(sourceType)
            ).stream().collect(groupingBy(DailyListEntity::getListingCourthouse));

            LogJobReport report = new LogJobReport(DAILY_LIST_JOB, dailyListsGroupedByCourthouse.size(), sourceType);
            try {
                dailyListsGroupedByCourthouse.forEach((listingCourthouse, dailyLists) -> {
                    processDailyListsForSourceType(dailyLists, report);
                });
            } finally {
                logApi.processedDailyListJob(report);
            }

        });
    }

    @Override
    public void processAllDailyListForListingCourthouse(String listingCourthouse) {
        stream(SourceType.values()).forEach(sourceType -> {
            var dailyLists = dailyListRepository.findByListingCourthouseAndStatusAndStartDateAndSourceOrderByPublishedTimestampDesc(
                listingCourthouse,
                JobStatusType.NEW,
                now(),
                String.valueOf(sourceType)
            );
            LogJobReport report = new LogJobReport(DAILY_LIST_JOB, dailyLists.size(), sourceType);

            try {
                processDailyListsForSourceType(dailyLists, report);
            } finally {
                logApi.processedDailyListJob(report);
            }
        });
    }


    private void processDailyListsForSourceType(List<DailyListEntity> dailyLists, LogJobReport report) {
        // Daily lists are being ordered descending by date so first item will be the most recent version

        if (!dailyLists.isEmpty()) {
            try {
                dailyListUpdater.processDailyList(dailyLists.get(0));

                // report on the daily list result
                report.registerResult(dailyLists.get(0).getStatus());
            } catch (JsonProcessingException | IllegalArgumentException e) {
                dailyLists.get(0).setStatus(JobStatusType.FAILED);
                report.registerFailed();
                log.error("Failed to process dailylist for dailylist id: {}", dailyLists.get(0).getId(), e);
            }

            if (dailyLists.size() > 1) {
                dailyLists.subList(1, dailyLists.size())
                    .forEach(dl -> {
                        dailyLists.get(0).setStatus(JobStatusType.IGNORED);
                        dl.setStatus(JobStatusType.IGNORED);
                    });
            }
            dailyListRepository.saveAll(dailyLists);
        }
    }
}