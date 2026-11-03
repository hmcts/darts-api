package uk.gov.hmcts.darts.cases.service;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.cases.model.CaseLinkImportResponse;

@FunctionalInterface
public interface CaseLinkImportService {

    CaseLinkImportResponse importCaseLinkingFile(MultipartFile file);
}
