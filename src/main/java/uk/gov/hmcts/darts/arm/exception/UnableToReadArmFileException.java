package uk.gov.hmcts.darts.arm.exception;

public class UnableToReadArmFileException extends Exception {
    private final String armFilename;

    public UnableToReadArmFileException(String armFilename, Throwable cause) {
        super(cause);
        this.armFilename = armFilename;
    }

    public UnableToReadArmFileException(String armFilename) {
        super("Unable to read ARM file: " + armFilename);
        this.armFilename = armFilename;
    }
}
