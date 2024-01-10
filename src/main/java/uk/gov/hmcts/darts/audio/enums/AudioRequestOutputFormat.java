package uk.gov.hmcts.darts.audio.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AudioRequestOutputFormat {

    MP3("MP3"),
    ZIP("ZIP");

    private final String extension;



}
