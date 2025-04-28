package uk.gov.hmcts.darts.arm.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class EodIdAndAssociatedFilenames {
    Long eodId;
    List<String> associatedFiles;

    void addAssociatedFile(String filename) {
        if (associatedFiles == null) {
            associatedFiles = new ArrayList<>();
        }
        associatedFiles.add(filename);
    }
}
