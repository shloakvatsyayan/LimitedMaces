package org.bonkmc.limitedmaces.updates.json;

final class JsonStringParser {
    private final String source;
    private int position;

    JsonStringParser(String source, int position) {
        this.source = source;
        this.position = position;
    }

    String parse() {
        expectOpeningQuote();
        StringBuilder parsedString = new StringBuilder();

        while (position < source.length()) {
            char character = source.charAt(position++);
            if (character == '"') {
                return parsedString.toString();
            }
            if (character == '\\') {
                parsedString.append(parseEscape());
                continue;
            }
            if (character < 0x20) {
                throw failure("Unescaped control character in string");
            }
            parsedString.append(character);
        }

        throw failure("Unterminated string");
    }

    int position() {
        return position;
    }

    private char parseEscape() {
        if (position >= source.length()) {
            throw failure("Unterminated escape sequence");
        }

        return switch (source.charAt(position++)) {
            case '"' -> '"';
            case '\\' -> '\\';
            case '/' -> '/';
            case 'b' -> '\b';
            case 'f' -> '\f';
            case 'n' -> '\n';
            case 'r' -> '\r';
            case 't' -> '\t';
            case 'u' -> parseUnicodeEscape();
            default -> throw failure("Invalid escape sequence");
        };
    }

    private char parseUnicodeEscape() {
        if (position + 4 > source.length()) {
            throw failure("Incomplete unicode escape");
        }

        int codePoint = 0;
        for (int index = 0; index < 4; index++) {
            int digit = Character.digit(source.charAt(position++), 16);
            if (digit < 0) {
                throw failure("Invalid unicode escape");
            }
            codePoint = codePoint * 16 + digit;
        }
        return (char) codePoint;
    }

    private void expectOpeningQuote() {
        if (position >= source.length() || source.charAt(position++) != '"') {
            throw failure("Expected '\"'");
        }
    }

    private IllegalArgumentException failure(String message) {
        return new IllegalArgumentException(message + " at position " + position);
    }
}
