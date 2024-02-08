package uk.gov.hmcts.darts.common.datamanagement.component.impl;

import lombok.RequiredArgsConstructor;
import uk.gov.hmcts.darts.common.datamanagement.component.MediaDownloadMetaData;
import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;

import java.io.InputStream;
import java.io.OutputStream;

@RequiredArgsConstructor
public class DefaultMediaDownloadMetaDataImpl implements MediaDownloadMetaData {

    protected final OutputStream os;

    private InputStream is;

    private boolean processed;

    private boolean successfulDownload;

    private DatastoreContainerType containerType;

    @Override
    public OutputStream getOutputStream() {
        return os;
    }

    @Override
    public void markProcessed() {
        processed = true;
    }

    @Override
    public void markSuccess(DatastoreContainerType containerType) {
        successfulDownload = true;
        this.containerType = containerType;
    }

    /**
     ** override as appropriate to get hold of your custom input stream.
     */
    public InputStream getInputStream() {
        return is;
    }

    @Override
    public void markInputStream(InputStream is) {
        this.is = is;
    }

    @Override
    public boolean isSuccessfullyDownloaded() {
        return successfulDownload;
    }

    @Override
    public boolean isProcessed() {
        return processed;
    }

    @Override
    public void undo() {
        // do not undo by default
    }
}