package uk.gov.hmcts.darts.audio.component.impl;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.audio.component.AudioMessageDigest;
import uk.gov.hmcts.darts.common.exception.DartsApiException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static uk.gov.hmcts.darts.audio.exception.AudioApiError.FAILED_TO_UPLOAD_AUDIO_FILE;

@RequiredArgsConstructor
public class AudioMessageDigestImpl implements AudioMessageDigest {

    private final String algorithm;

    @Override
    public java.security.MessageDigest getMessageDigest() {
        java.security.MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            throw new DartsApiException(FAILED_TO_UPLOAD_AUDIO_FILE, e);
        }
        return digest;
    }
}