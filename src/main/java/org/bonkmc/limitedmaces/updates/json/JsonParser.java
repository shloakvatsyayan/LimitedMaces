package org.bonkmc.limitedmaces.updates.json;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonParser {
    private final String source;
    private int position;

    private JsonParser(String source) {
        this.source = source;
    }

    public static Object parse(String source) {
        if (source == null) {
            throw new IllegalArgumentException("JSON source cannot be null");
        }
        return new JsonParser(source).parseDocument();
    }

    private Object parseDocument() {
        skipWhitespace();
        Object parsedElement = parseElement();
        skipWhitespace();
        if (position != source.length()) {
            throw failure("Unexpected trailing content");
        }
        return parsedElement;
    }

    private Object parseElement() {
        if (position >= source.length()) {
            throw failure("Expected a JSON value");
        }
        return switch (source.charAt(position)) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> parseString();
            case 't' -> parseLiteral("true", true);
            case 'f' -> parseLiteral("false", false);
            case 'n' -> parseLiteral("null", null);
            default -> parseNumber();
        };
    }

    private Map<String, Object> parseObject() {
        expect('{');
        Map<String, Object> parsedObject = new LinkedHashMap<>();
        skipWhitespace();
        if (consume('}')) {
            return parsedObject;
        }
        while (true) {
            skipWhitespace();
            if (!peek('"')) {
                throw failure("Expected an object key");
            }
            String key = parseString();
            skipWhitespace();
            expect(':');
            skipWhitespace();
            parsedObject.put(key, parseElement());
            skipWhitespace();
            if (consume('}')) {
                return parsedObject;
            }
            expect(',');
        }
    }

    private List<Object> parseArray() {
        expect('[');
        List<Object> parsedArray = new ArrayList<>();
        skipWhitespace();
        if (consume(']')) {
            return parsedArray;
        }
        while (true) {
            skipWhitespace();
            parsedArray.add(parseElement());
            skipWhitespace();
            if (consume(']')) {
                return parsedArray;
            }
            expect(',');
        }
    }

    private String parseString() {
        JsonStringParser stringParser = new JsonStringParser(source, position);
        String parsedString = stringParser.parse();
        position = stringParser.position();
        return parsedString;
    }

    private BigDecimal parseNumber() {
        int start = position;
        consume('-');
        if (consume('0')) {
            if (position < source.length() && Character.isDigit(source.charAt(position))) {
                throw failure("Leading zero in number");
            }
        } else {
            consumeDigits("Expected a number");
        }

        if (consume('.')) {
            consumeDigits("Expected digits after decimal point");
        }
        if (consume('e') || consume('E')) {
            consume('+');
            consume('-');
            consumeDigits("Expected exponent digits");
        }
        try {
            return new BigDecimal(source.substring(start, position));
        } catch (NumberFormatException exception) {
            throw failure("Invalid number");
        }
    }

    private Object parseLiteral(String literal, Object parsedLiteral) {
        if (!source.startsWith(literal, position)) {
            throw failure("Invalid literal");
        }
        position += literal.length();
        return parsedLiteral;
    }

    private void consumeDigits(String failureMessage) {
        int start = position;
        while (position < source.length() && Character.isDigit(source.charAt(position))) {
            position++;
        }
        if (start == position) {
            throw failure(failureMessage);
        }
    }

    private void skipWhitespace() {
        while (position < source.length() && Character.isWhitespace(source.charAt(position))) {
            position++;
        }
    }

    private void expect(char expected) {
        if (!consume(expected)) {
            throw failure("Expected '" + expected + "'");
        }
    }

    private boolean consume(char expected) {
        if (!peek(expected)) {
            return false;
        }
        position++;
        return true;
    }

    private boolean peek(char expected) {
        return position < source.length() && source.charAt(position) == expected;
    }

    private IllegalArgumentException failure(String message) {
        return new IllegalArgumentException(message + " at position " + position);
    }
}
