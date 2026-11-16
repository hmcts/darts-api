package uk.gov.hmcts.darts.cases.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Component
@ConfigurationProperties(prefix = "darts.cases.link-import")
@Validated
@Getter
@Setter
public class CaseLinkImportConfiguration {

    @NotNull
    private DataSize maxFileSize;

    @Min(1)
    private int maxRows;

    @Min(1)
    private int maxRecordCharacters;

    @Min(1)
    private int failurePageSize;

    @Min(1)
    private int auditBatchSize;
}
