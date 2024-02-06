package uk.gov.hmcts.darts.audit.controller;

import jakarta.validation.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.DataBinder;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.darts.audit.AuditSearchQueryValidator;
import uk.gov.hmcts.darts.audit.controller.mapper.AuditDtoMapper;
import uk.gov.hmcts.darts.audit.http.api.AuditApi;
import uk.gov.hmcts.darts.audit.model.AuditSearchQuery;
import uk.gov.hmcts.darts.audit.model.SearchResult;
import uk.gov.hmcts.darts.audit.service.AuditService;
import uk.gov.hmcts.darts.common.entity.AuditEntity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AuditController implements AuditApi {

    private final AuditService auditService;
    private final AuditDtoMapper auditDtoMapper;
    private final AuditSearchQueryValidator validator;

    @Override
    public ResponseEntity<List<SearchResult>> search(OffsetDateTime fromDate, OffsetDateTime toDate, Integer caseId, Integer auditActivityId) {
        AuditSearchQuery searchQuery = new AuditSearchQuery();
        searchQuery.setCaseId(caseId);
        searchQuery.setFromDate(fromDate);
        searchQuery.setToDate(toDate);
        searchQuery.setAuditActivityId(auditActivityId);

        validate(searchQuery);
        List<AuditEntity> searchResults = auditService.search(searchQuery);
        List<SearchResult> responseResults = auditDtoMapper.mapToSearchResult(searchResults);

        return new ResponseEntity<>(responseResults, HttpStatus.OK);
    }

    private void validate(AuditSearchQuery searchQuery) {
        DataBinder dataBinder = new DataBinder(searchQuery);
        dataBinder.addValidators(validator);
        dataBinder.validate();

        List<ObjectError> errors = dataBinder.getBindingResult().getAllErrors();
        if (!errors.isEmpty()) {
            String validationMessages = errors
                  .stream()
                  .map(objectError -> objectError.getDefaultMessage() + " ")
                  .collect(Collectors.joining(", "));

            throw new ValidationException(validationMessages);
        }
    }
}
