package uk.gov.hmcts.darts.common.datamanagement.api;

import uk.gov.hmcts.darts.common.datamanagement.component.impl.DownloadableExternalObjectDirectories;

/**
 * An interface that mediates between the three data management APIs.
 * {@link uk.gov.hmcts.darts.datamanagement.api.DataManagementApi}
 * {@link uk.gov.hmcts.darts.dets.api.DetsDataManagementApi}
 * {@link uk.gov.hmcts.darts.arm.api.ArmDataManagementApi}
 */
public interface DataManagementFacade {

    /**
     * process a collection of downloads in the order of unstructured, dets (if boolean set) and arm. All downloads are attempted
     * even if one fails
     * @param directories The external directories to be processed that are passed by reference i.e. the
     *                    object responses are updated with the outcome of the processing state.
     */
    void getDataFromUnstructuredArmAndDetsBlobs(DownloadableExternalObjectDirectories directories);

}