package uk.gov.hmcts.darts.arm.exception;

import lombok.AllArgsConstructor;

@AllArgsConstructor
public class UnableToReadArmFileException extends Exception {
    String armFilename;

    public UnableToReadArmFileException(String armFilename, Throwable cause) {
        super(cause);
        this.armFilename = armFilename;
    }

    public UnableToReadArmFileException() {
        super();
    }
}
