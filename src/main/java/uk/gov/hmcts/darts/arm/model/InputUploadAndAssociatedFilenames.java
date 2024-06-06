package uk.gov.hmcts.darts.arm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class InputUploadAndAssociatedFilenames {
    String inputUploadFilename;
    List<String> associatedFiles;
}
