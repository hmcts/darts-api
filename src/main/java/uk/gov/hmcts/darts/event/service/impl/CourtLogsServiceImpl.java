package uk.gov.hmcts.darts.event.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.EventEntity;
import uk.gov.hmcts.darts.common.repository.CourtLogEventRepository;
import uk.gov.hmcts.darts.event.mapper.EventEntityToCourtLogMapper;
import uk.gov.hmcts.darts.event.model.CourtLog;
import uk.gov.hmcts.darts.event.service.CourtLogsService;

import java.time.OffsetDateTime;
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class CourtLogsServiceImpl implements CourtLogsService {

    private final CourtLogEventRepository repository;

    @Override
    public List<CourtLog> getCourtLogs(String courtHouse, String caseNumber, OffsetDateTime start, OffsetDateTime end) {
        String courthouseNameUpperTrimmed = StringUtils.toRootUpperCase(StringUtils.trimToEmpty(courtHouse));

        List<EventEntity> entities = repository.findByCourthouseAndCaseNumberBetweenStartAndEnd(
            courthouseNameUpperTrimmed,
            caseNumber,
            start,
            end
        );
        return EventEntityToCourtLogMapper.mapToCourtLogsList(entities);

    }
}

