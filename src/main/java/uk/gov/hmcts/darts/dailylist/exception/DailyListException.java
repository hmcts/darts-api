package uk.gov.hmcts.darts.dailylist.exception;

public class DailyListException extends RuntimeException {
    public DailyListException(String message, Throwable cause) {
        super(message, cause);
    }

    public DailyListException(String message) {
        super(message);
    }

    public DailyListException(Throwable cause) {
        super(cause);
    }
}
