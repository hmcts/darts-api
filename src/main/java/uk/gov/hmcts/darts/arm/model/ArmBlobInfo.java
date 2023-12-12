package uk.gov.hmcts.darts.arm.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ArmBlobInfo {
    private String blobPathAndName;
    private String blobName;
}
