package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;

import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
@Getter
public class ResponseMetaData {
    private InputStream is;

    protected OutputStream outputStream;

    @Getter
    private boolean processed;

    private boolean successfulDownload;

    @Getter
    private DatastoreContainerType containerType;

    public void markProcessed() {
        processed = true;
    }


    public void markSuccess(DatastoreContainerType type) {
        successfulDownload = true;
        this.containerType = type;
    }

    /**
     ** override as appropriate to return your custom input stream null by default.
     */
    public InputStream getInputStream() {
        return is;
    }

    public void markInputStream(InputStream is) {
        this.is = is;
    }


    public boolean isSuccessfullyDownloaded() {
        return successfulDownload;
    }
}