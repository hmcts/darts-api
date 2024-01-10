package uk.gov.hmcts.darts.audio.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AudioRequestOutputFormat {

    MP3("mp3"),
    ZIP("zip");

    private final String extension;

}
