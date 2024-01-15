package uk.gov.hmcts.darts.retentions.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.common.entity.CourtCaseEntity;
import uk.gov.hmcts.darts.common.exception.DartsApiException;
import uk.gov.hmcts.darts.common.repository.CaseRepository;
import uk.gov.hmcts.darts.retentions.exception.RetentionsApiError;
import uk.gov.hmcts.darts.retentions.model.PostRetentionRequest;
import uk.gov.hmcts.darts.retentions.service.RetentionsService;

import java.text.MessageFormat;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetentionsServiceImpl implements RetentionsService {

    private final CaseRepository caseRepository;

    @Override
    public void postRetention(PostRetentionRequest postRetentionRequest) {

        Optional<CourtCaseEntity> caseOpt = caseRepository.findById(postRetentionRequest.getCaseId());
        if (caseOpt.isEmpty()) {
            throw new DartsApiException(
                RetentionsApiError.CASE_NOT_FOUND,
                MessageFormat.format("The selected caseId ''{0}'' cannot be found.", postRetentionRequest.getCaseId())
            );
        }

        CourtCaseEntity courtCase = caseOpt.get();



        courtCase.get

    }

    private void validation(PostRetentionRequest postRetentionRequest, CourtCaseEntity courtCase){
        //No retention can be applied/amended when then case is open
        if(BooleanUtils.isNotTrue(courtCase.getClosed())){
            throw new DartsApiException(
                RetentionsApiError.CASE_NOT_CLOSED,
                MessageFormat.format("caseId ''{0}'' must be closed before the retention period can be amended.", courtCase.getId())
            );
        }

        //No retention can be applied/amended when no current retention policy has been applied
        if(BooleanUtils.isNotTrue(courtCase.getReClosed())){
            throw new DartsApiException(
                RetentionsApiError.CASE_NOT_CLOSED,
                MessageFormat.format("caseId ''{0}'' must be closed before the retention period can be amended.", courtCase.getId())
            );
        }
    }
}
