package uk.gov.hmcts.darts.arm.model.blobs;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Builder
@Getter
@Setter
public class ContinuationTokenBlobs {
    private List<String> blobNamesWithAndPaths;
    private String continuationToken;

}
