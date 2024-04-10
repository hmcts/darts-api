package uk.gov.hmcts.darts.arm.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import uk.gov.hmcts.darts.arm.util.files.CreateRecordFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.InvalidLineFileFilenameProcessor;
import uk.gov.hmcts.darts.arm.util.files.UploadFileFilenameProcessor;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ResponseFilenames {
    List<CreateRecordFilenameProcessor> createRecordResponses = new ArrayList<>();
    List<UploadFileFilenameProcessor> uploadFileResponses = new ArrayList<>();
    List<InvalidLineFileFilenameProcessor> invalidLineResponses = new ArrayList<>();

    public boolean containsResponses() {
        return CollectionUtils.isNotEmpty(createRecordResponses)
            || CollectionUtils.isNotEmpty(uploadFileResponses)
            || CollectionUtils.isNotEmpty(invalidLineResponses);
    }


}
