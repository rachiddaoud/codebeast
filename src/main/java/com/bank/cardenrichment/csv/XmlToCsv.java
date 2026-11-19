package com.bank.cardenrichment.csv;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Turns repeated XML elements into CSV rows.
 * <p>
 * Each {@link Column} reads one value with an XPath relative to the record element
 * ({@code name}, {@code merchant/city}, {@code @id}), optionally transforms it, and
 * the CSV columns come out in the order the columns are given.
 */
public final class XmlToCsv {

    public record Column(String header, String path, UnaryOperator<String> transform) {

        public static Column of(String header, String path) {
            return new Column(header, path, UnaryOperator.identity());
        }

        public static Column of(String header, String path, UnaryOperator<String> transform) {
            return new Column(header, path, transform);
        }
    }

    private static final XPathFactory XPATH = XPathFactory.newInstance();

    private final XPathExpression records;
    private final List<Column> columns;
    private final List<XPathExpression> paths;

    /**
     * @param recordPath XPath selecting one element per CSV row, e.g. {@code //transaction}
     */
    public XmlToCsv(String recordPath, List<Column> columns) {
        this.records = compile(recordPath);
        this.columns = List.copyOf(columns);
        this.paths = this.columns.stream().map(c -> compile(c.path())).toList();
    }

    public String convert(InputStream xml) {
        StringWriter out = new StringWriter();
        convert(xml, out);
        return out.toString();
    }

    public void convert(InputStream xml, Writer out) {
        try {
            NodeList rows = (NodeList) records.evaluate(parse(xml), XPathConstants.NODESET);
            writeLine(out, columns.stream().map(Column::header).toList());
            for (int i = 0; i < rows.getLength(); i++) {
                writeLine(out, row(rows.item(i)));
            }
            out.flush();
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Invalid XPath", e);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private List<String> row(Node record) throws XPathExpressionException {
        String[] values = new String[columns.size()];
        for (int i = 0; i < values.length; i++) {
            String raw = paths.get(i).evaluate(record).strip();
            values[i] = columns.get(i).transform().apply(raw);
        }
        return List.of(values);
    }

    private static void writeLine(Writer out, List<String> values) throws IOException {
        out.write(values.stream().map(XmlToCsv::escape).collect(Collectors.joining(",")));
        out.write("\r\n");
    }

    /** RFC 4180: quote when the value holds a comma, quote or line break; double inner quotes. */
    static String escape(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }

    private static Document parse(InputStream xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            // No DTDs, no external entities: input may come from outside.
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            return factory.newDocumentBuilder().parse(xml);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid XML", e);
        }
    }

    private static XPathExpression compile(String path) {
        try {
            XPath xpath = XPATH.newXPath();
            return xpath.compile(path);
        } catch (XPathExpressionException e) {
            throw new IllegalArgumentException("Invalid XPath: " + path, e);
        }
    }
}
