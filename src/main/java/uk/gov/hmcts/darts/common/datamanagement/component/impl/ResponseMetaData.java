package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
@Getter
public class ResponseMetaData {
    private InputStream is;

    private final OutputStream outputStream;

    @Getter
    private boolean processed;

    private boolean successfulDownload;

    @Getter
    private DatastoreContainerType containerType;

    public void markProcessed(DatastoreContainerType type) {
        processed = true;
        this.containerType = type;
    }


    public void markSuccess() {
        successfulDownload = true;
    }

    /**
     ** override as appropriate to return your custom input stream null by default.
     */
    public InputStream getInputStream()  throws IOException {
        return is;
    }

    public void markInputStream(InputStream is) {
        this.is = is;
    }


    public boolean isSuccessfullyDownloaded() {
        return successfulDownload;
    }
}