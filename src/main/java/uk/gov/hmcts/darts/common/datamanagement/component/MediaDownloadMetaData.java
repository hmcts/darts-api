package uk.gov.hmcts.darts.common.datamanagement.component;

import uk.gov.hmcts.darts.common.datamanagement.enums.DatastoreContainerType;

import java.io.InputStream;
import java.io.OutputStream;

public interface MediaDownloadMetaData {

    /**
     * The output stream to which the request will be written to.
     * @return the output stream
     */
    OutputStream getOutputStream();

    /**
     * gets the input stream of the output stream.
     * @return the input stream
     */
    InputStream getInputStream();

    /**
     * The ability for clients to delete the media that has been downloaded. Would
     * only do anything if the download was successfully written to the output stream.
     */
    void undo();

    /**
     * Marks this downoad a successful to the container.
     * @param containerType The container name where the data exists
     */
    void markSuccess(DatastoreContainerType containerType);

    /**
     * Marks if the download was attempted.
     */
    void markProcessed();

    /**
     * marks up with an input stream for the download.
     */
    void markInputStream(InputStream is);

    /**
     * was a download attempted and if so was it successful.
     */
    boolean isSuccessfullyDownloaded();

    /**
     * If the download has been processed. Processing state allows us to differentiate between whether a
     * download had been attempted.
     */
    boolean isProcessed();
}