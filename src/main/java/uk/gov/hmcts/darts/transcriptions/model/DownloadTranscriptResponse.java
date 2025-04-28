package uk.gov.hmcts.darts.transcriptions.model;

import lombok.Builder;
import lombok.Value;
import org.springframework.core.io.Resource;

@Builder
@Value
public class DownloadTranscriptResponse {

    private Resource resource;
    private String fileName;
    private String contentType;
    private Long transcriptionDocumentId;

}
