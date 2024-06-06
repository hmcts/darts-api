package uk.gov.hmcts.darts.arm.model.record.armresponse;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ArmResponseEodFile {
    Integer eodId;
    String armFilename;
}
