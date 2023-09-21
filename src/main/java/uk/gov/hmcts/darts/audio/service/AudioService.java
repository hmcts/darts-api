package uk.gov.hmcts.darts.audio.service;

import java.io.InputStream;

public interface AudioService {

    InputStream download(Integer mediaRequestId);

    InputStream preview(Integer mediaId);

}
