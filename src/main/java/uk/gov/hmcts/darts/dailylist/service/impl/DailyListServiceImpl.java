package uk.gov.hmcts.darts.dailylist.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.Courthouse;
import uk.gov.hmcts.darts.common.entity.DailyListEntity;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseCodeNotMatchException;
import uk.gov.hmcts.darts.courthouse.exception.CourthouseNameNotFoundException;
import uk.gov.hmcts.darts.courthouse.service.CourthouseService;
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
    private final CourthouseService courthouseService;


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
            DailyListMapper.mapToExistingDailyListEntity(postRequest, courthouse, existingRecord);
            dailyListRepository.saveAndFlush(existingRecord);
        } else {
            //insert new record
            DailyListEntity dailyListEntity = DailyListMapper.mapToDailyListEntity(
                postRequest,
                courthouse
            );
            dailyListRepository.saveAndFlush(dailyListEntity);
        }

    }

    private Courthouse retrieveCourtHouse(DailyList dailyList) {
        CourtHouse crownCourt = dailyList.getCrownCourt();
        Short courthouseCode = crownCourt.getCourtHouseCode().getCode().shortValue();
        String courthouseName = crownCourt.getCourtHouseName();
        try{
            return courthouseService.retrieveCourtHouse(courthouseCode, courthouseName);
        } catch (CourthouseCodeNotMatchException ccnme) {
            log.warn(
                "Courthouse {} has code {} but dailyList says it should be {}",
                courthouseName,
                ccnme.getCourthouse().getCode(),
                courthouseCode
            );
            return ccnme.getCourthouse();
        } catch (CourthouseNameNotFoundException e) {
            String message = MessageFormat.format(
                "DailyList with uniqueId {0} received with an invalid courthouse ''{1}''",
                dailyList.getDocumentId().getUniqueId(),
                crownCourt.getCourtHouseName()
            );
            throw new RuntimeException(message);
        }
    }

}
