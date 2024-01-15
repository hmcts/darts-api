package uk.gov.hmcts.darts.retention.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.retention.service.RetentionService;
import uk.gov.hmcts.darts.retentions.http.api.RetentionApi;
import uk.gov.hmcts.darts.retentions.model.CaseRetention;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class RetentionController implements RetentionApi {

    private final RetentionService retentionService;

    @Override
    public ResponseEntity<List<CaseRetention>> retentionsGet(Integer caseId) {
        return new ResponseEntity<>(retentionService.getCaseRetentions(caseId), HttpStatus.OK);
    }
}
