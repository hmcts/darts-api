package uk.gov.hmcts.darts.audio.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import uk.gov.hmcts.darts.audio.enums.AudioPreviewStatus;

import java.io.Serializable;

@ToString
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode()
@Getter
public class AudioPreview implements Serializable {
    private Long mediaId;

    private AudioPreviewStatus status;

    private byte[] audio;
}