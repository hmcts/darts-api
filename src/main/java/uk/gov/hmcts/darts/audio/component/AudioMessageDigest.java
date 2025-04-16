package uk.gov.hmcts.darts.audio.component;

import java.security.MessageDigest;

@FunctionalInterface
public interface AudioMessageDigest {
    MessageDigest getMessageDigest();
}