package uk.gov.hmcts.darts.cases.service;

import org.springframework.web.multipart.MultipartFile;
import uk.gov.hmcts.darts.common.entity.CaseLinkImportResultEntity;

import java.util.List;

@FunctionalInterface
public interface CaseLinkCsvService {

    List<CaseLinkImportResultEntity> readCaseLinkCsvFile(MultipartFile file);
}
