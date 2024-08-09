package uk.gov.hmcts.darts.common.config.security;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.KeySourceException;

public interface JwksInitialize {
    void init() throws KeySourceException, JOSEException;
}