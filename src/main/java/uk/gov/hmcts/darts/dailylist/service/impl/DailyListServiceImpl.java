package uk.gov.hmcts.darts.dailylist.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.courthouse.api.CourthouseApi;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.dailylist.exception.DailyListException;
import uk.gov.hmcts.darts.dailylist.mapper.DailyListMapper;
import uk.gov.hmcts.darts.dailylist.model.CourtHouse;
import uk.gov.hmcts.darts.dailylist.model.DailyList;
import uk.gov.hmcts.darts.dailylist.model.DailyListPostRequest;
import uk.gov.hmcts.darts.dailylist.repository.DailyListRepository;
import uk.gov.hmcts.darts.dailylist.service.DailyListService;

import java.text.MessageFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class DailyListServiceImpl implements DailyListService {

    private final DailyListRepository dailyListRepository;
    private final CourthouseApi courthouseApi;
    private final DailyListMapper dailyListMapper;


    @Override
    /*
    Retrieve the new Daily List, and store it in the database.
     */
    public void processIncomingDailyList(DailyListPostRequest postRequest) {
        DailyList dailyList = postRequest.getDailyList();

        dailyList.getCrownCourt().getCourtHouseName();
        Courthouse courthouse = retrieveCourtHouse(dailyList);
        String uniqueId = dailyList.getDocumentId().getUniqueId();
        Optional<DailyListEntity> existingRecordOpt = dailyListRepository.findByUniqueId(uniqueId);
        if (existingRecordOpt.isPresent()) {
            //update the record
            DailyListEntity existingRecord = existingRecordOpt.get();
            dailyListMapper.mapToExistingDailyListEntity(postRequest, courthouse, existingRecord);
            dailyListRepository.saveAndFlush(existingRecord);
        } else {
            //insert new record
            DailyListEntity dailyListEntity = dailyListMapper.mapToDailyListEntity(
                postRequest,
                courthouse
            );
            dailyListRepository.saveAndFlush(dailyListEntity);
        }

    }

    private Courthouse retrieveCourtHouse(DailyList dailyList) {
        CourtHouse crownCourt = dailyList.getCrownCourt();
        Integer courthouseCode = crownCourt.getCourtHouseCode().getCode();
        String courthouseName = crownCourt.getCourtHouseName();
        try {
            return courthouseApi.retrieveAndUpdateCourtHouse(courthouseCode, courthouseName);
        } catch (CourthouseCodeNotMatchException ccnme) {
            log.warn(
                "Courthouse in database {} Does not match that received by dailyList, {} {}",
                ccnme.getDatabaseCourthouse(),
                courthouseCode,
                courthouseName
            );
            return ccnme.getDatabaseCourthouse();
        } catch (CourthouseNameNotFoundException e) {
            String message = MessageFormat.format(
                "DailyList with uniqueId {0} received with an invalid courthouse ''{1}''",
                dailyList.getDocumentId().getUniqueId(),
                crownCourt.getCourtHouseName()
            );
            throw new DailyListException(message, e);
        }
    }

}
