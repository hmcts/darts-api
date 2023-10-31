package uk.gov.hmcts.darts.transcriptions.model;

import lombok.Builder;
import lombok.Value;
import org.springframework.core.io.Resource;

import java.util.UUID;

@Builder
@Value
public class DownloadTranscriptResponse {

    private Resource resource;
    private String fileName;
    private UUID externalLocation;
    private Integer transcriptionDocumentId;

}
