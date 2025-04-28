package uk.gov.hmcts.darts.arm.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class InputUploadAndAssociatedFilenames {
    String inputUploadFilename;
    List<EodIdAndAssociatedFilenames> eodIdAndAssociatedFilenamesList = new ArrayList<>();

    public void addAssociatedFile(Long eodId, String filename) {
        for (EodIdAndAssociatedFilenames eodIdAndAssociatedFilenames : eodIdAndAssociatedFilenamesList) {
            if (eodId.equals(eodIdAndAssociatedFilenames.getEodId())) {
                eodIdAndAssociatedFilenames.addAssociatedFile(filename);
                return;
            }
        }

        //only reaches here if no match found
        List<String> filenameList = new ArrayList<>();
        filenameList.add(filename);
        eodIdAndAssociatedFilenamesList.add(new EodIdAndAssociatedFilenames(eodId, filenameList));
    }
}
