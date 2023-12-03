package uk.gov.hmcts.darts.archiverecordsmanagement.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.darts.common.exception.DartsApiError;

@Getter
@RequiredArgsConstructor
public enum ArchiveRecordApiError implements DartsApiError {
    FAILED_TO_GENERATE_MEDIA_ARCHIVE_RECORD(
        "100",
        null,
        "Failed to generate media archive record"
    );

    private static final String ERROR_TYPE_PREFIX = "ArchiveRecord";

    private final String errorTypeNumeric;
    private final HttpStatus httpStatus;
    private final String title;

    @Override
    public String getErrorTypePrefix() {
        return ERROR_TYPE_PREFIX;
    }
}
