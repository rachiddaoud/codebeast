package com.bank.cardenrichment.csv;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Writes objects as CSV (RFC 4180), one column per {@link Column}, in the order given. */
public final class Csv {

    /** A header and how to get the cell from a row. A null value gives an empty cell. */
    public record Column<T>(String header, Function<T, ?> value) {

        public static <T> Column<T> of(String header, Function<T, ?> value) {
            return new Column<>(header, value);
        }
    }

    private Csv() {
    }

    public static <T> void write(List<Column<T>> columns, List<T> rows, Writer out) {
        try {
            writeLine(out, columns.stream().map(Column::header));
            for (T row : rows) {
                writeLine(out, columns.stream().map(c -> c.value().apply(row)));
            }
            out.flush();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void writeLine(Writer out, Stream<?> values) throws IOException {
        out.write(values.map(Csv::escape).collect(Collectors.joining(",")));
        out.write("\r\n");
    }

    /** Quotes a value holding a comma, quote or line break, and doubles its quotes. */
    static String escape(Object value) {
        if (value == null) {
            return "";
        }
        String s = value.toString();
        if (s.contains(",") || s.contains("\"") || s.contains("\n") || s.contains("\r")) {
            return '"' + s.replace("\"", "\"\"") + '"';
        }
        return s;
    }
}
