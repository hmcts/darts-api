package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.event.exception.DarNotifyError;
import uk.gov.hmcts.darts.event.helper.DarNotifyAsyncHelper;
import uk.gov.hmcts.darts.event.model.DarNotifyApplicationEvent;
import uk.gov.hmcts.darts.util.DataUtil;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DarNotifyServiceImpl {

    private final CaseRepository caseRepository;
    private final DarNotifyAsyncHelper darNotifyAsyncHelper;

    @Async
    @EventListener
    public void onApplicationEvent(DarNotifyApplicationEvent event) throws DarNotifyError {
        notifyDarPc(event);
    }

    public void notifyDarPc(DarNotifyApplicationEvent event) throws DarNotifyError {
        var dartsEvent = event.getDartsEvent();
        DataUtil.preProcess(dartsEvent);
        List<String> openCaseNumbers = caseRepository.findOpenCaseNumbers(dartsEvent.getCourthouse(), dartsEvent.getCaseNumbers());
        if (openCaseNumbers.isEmpty()) {
            log.info("DarNotify ignored, no open cases: event_id={}, courthouse={}",
                     DataUtil.toUpperCase(dartsEvent.getCourthouse()),
                     event.getDartsEvent().getEventId());
        } else {
            darNotifyAsyncHelper.notifyDarPcAsync(event, openCaseNumbers);
        }
    }


}
