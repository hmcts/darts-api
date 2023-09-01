package uk.gov.hmcts.darts.hearings.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.HearingEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.HearingRepository;
import uk.gov.hmcts.darts.hearings.exception.HearingApiError;
import uk.gov.hmcts.darts.hearings.mapper.GetHearingResponseMapper;
import uk.gov.hmcts.darts.hearings.model.GetHearingResponse;
import uk.gov.hmcts.darts.hearings.service.HearingsService;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class HearingsServiceImpl implements HearingsService {

    private final HearingRepository hearingRepository;

    @Override
    public GetHearingResponse getHearings(Integer hearingId) {
        Optional<HearingEntity> foundHearingOpt = hearingRepository.findById(hearingId);
        if (foundHearingOpt.isEmpty()) {
            throw new DartsApiException(HearingApiError.HEARING_NOT_FOUND);
        }
        return GetHearingResponseMapper.map(foundHearingOpt.get());
    }
}
