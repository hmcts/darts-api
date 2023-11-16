package uk.gov.hmcts.darts.audio.service.impl;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.darts.audio.service.ExternalDataStoreDeleter;
import uk.gov.hmcts.darts.audio.service.InboundUnstructuredDataStoreDeleter;

@Service
@RequiredArgsConstructor
public class ExternalDataStoreDeleterImpl implements ExternalDataStoreDeleter {


    private final InboundUnstructuredDataStoreDeleter inboundUnstructuredDataStoreDeleter;
    private final OutboundDataStoreDeleterImpl outboundDataStoreDeleter;


    @Override
    @Transactional
    public void delete() {
        inboundUnstructuredDataStoreDeleter.delete();
        outboundDataStoreDeleter.delete();
    }
}
