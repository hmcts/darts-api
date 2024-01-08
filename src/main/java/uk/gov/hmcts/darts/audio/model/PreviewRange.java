package uk.gov.hmcts.darts.audio.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class PreviewRange {

    long startRange;
    long endRange;
    long contentLength;
}
