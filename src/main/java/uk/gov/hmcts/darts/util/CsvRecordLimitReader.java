package uk.gov.hmcts.darts.util;

import java.io.IOException;
import java.io.Reader;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Limits raw characters per CSV record before a parser can materialise its fields.
 * Supports the comma delimiter and doubled double quotes used by {@code CSVFormat.DEFAULT}.
 */
public class CsvRecordLimitReader extends Reader {

    private static final char DELIMITER = ',';
    private static final char QUOTE = '"';
    private static final char CARRIAGE_RETURN = '\r';
    private static final char LINE_FEED = '\n';

    private final Reader reader;
    private final int maxRecordCharacters;
    private final Supplier<? extends RuntimeException> limitExceptionSupplier;
    private int recordCharacters;
    private FieldState fieldState = FieldState.START;

    public CsvRecordLimitReader(Reader reader, int maxRecordCharacters, Supplier<? extends RuntimeException> limitExceptionSupplier) {
        super(reader);
        if (maxRecordCharacters < 1) {
            throw new IllegalArgumentException("maxRecordCharacters must be positive");
        }
        this.reader = reader;
        this.maxRecordCharacters = maxRecordCharacters;
        this.limitExceptionSupplier = Objects.requireNonNull(limitExceptionSupplier);
    }

    @Override
    public int read(char[] characters, int offset, int length) throws IOException {
        int charactersRead = reader.read(characters, offset, length);
        for (int index = offset; index < offset + charactersRead; index++) {
            checkCharacter(characters[index]);
        }
        return charactersRead;
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }

    private void checkCharacter(char character) {
        if ((character == CARRIAGE_RETURN || character == LINE_FEED) && fieldState != FieldState.QUOTED) {
            recordCharacters = 0;
            fieldState = FieldState.START;
            return;
        }
        if (recordCharacters == maxRecordCharacters) {
            throw limitExceptionSupplier.get();
        }
        recordCharacters++;
        fieldState = nextFieldState(character);
    }

    private FieldState nextFieldState(char character) {
        return switch (fieldState) {
            case START -> startField(character);
            case UNQUOTED -> character == DELIMITER ? FieldState.START : FieldState.UNQUOTED;
            case QUOTED -> character == QUOTE ? FieldState.AFTER_QUOTE : FieldState.QUOTED;
            case AFTER_QUOTE -> afterQuote(character);
        };
    }

    private FieldState startField(char character) {
        if (character == QUOTE) {
            return FieldState.QUOTED;
        }
        return character == DELIMITER ? FieldState.START : FieldState.UNQUOTED;
    }

    private FieldState afterQuote(char character) {
        if (character == QUOTE) {
            return FieldState.QUOTED;
        }
        return character == DELIMITER ? FieldState.START : FieldState.AFTER_QUOTE;
    }

    private enum FieldState {
        START, UNQUOTED, QUOTED, AFTER_QUOTE
    }
}
