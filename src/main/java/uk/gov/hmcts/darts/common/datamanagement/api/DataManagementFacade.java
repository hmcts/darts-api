package uk.gov.hmcts.darts.common.datamanagement.api;

import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadableExternalObjectDirectory;

import java.util.Collection;
import java.util.function.Function;

/**
 * An interface that mediates between the three data management APIs.
 * {@link uk.gov.hmcts.darts.datamanagement.api.DataManagementApi}
 * {@link uk.gov.hmcts.darts.dets.api.DetsDataManagementApi}
 * {@link uk.gov.hmcts.darts.arm.api.ArmDataManagementApi}
 */
public interface DataManagementFacade {

    /**
     * process a collection of downloads in the order of unstructured, dets (if boolean set) and arm as a fallback.
     * @param directories The external directories to be processed that are passed by reference i.e. these
     *                    objects are updated with the outcome of the processing state.
     * @param isFetchfromDets Determine if we need to reach out to dets.
     * @param handlePostDownload The processing as it occurs. This gets called regardless of whether the download was successful.
     *                           Allows the caller to terminate downloads at any point by returning a boolean
     */
    void getDataFromUnstructuredArmAndDetsBlobs(Collection<DownloadableExternalObjectDirectory> directories,
                                                boolean isFetchfromDets,
                                                Function<DownloadableExternalObjectDirectory, Boolean> handlePostDownload);

    /**
     * Returns the input streams representing the data for the directories. DETS communication is enabled by default.
     * @param directories The external directories to be processed that are passed by reference i.e. these
     *                    objects are updated with the processing state
     * @param handlePostDownload The processing as it occurs. This gets called regardless of whether the download was successful.
     *                           Allows the caller to terminate downloads at any point by returning a boolean
     */
    void getDataFromUnstructuredArmAndDetsBlobs(Collection<DownloadableExternalObjectDirectory> directories,
                                                Function<DownloadableExternalObjectDirectory, Boolean> handlePostDownload);
}