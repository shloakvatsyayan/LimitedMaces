package org.bonkmc.limitedmaces.updates.json;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class JsonParserTest {
    @Test
    void parsesNestedJsonWithEscapesAndNumbers() {
        Object parsedDocument = JsonParser.parse("""
                {
                  "name": "Limited\\u004daces",
                  "versions": [true, null, 2.01e2]
                }
                """);

        assertEquals(
                Map.of(
                        "name", "LimitedMaces",
                        "versions", Arrays.asList(true, null, new BigDecimal("2.01e2"))
                ),
                parsedDocument
        );
    }

    @Test
    void rejectsTrailingContent() {
        assertThrows(
                IllegalArgumentException.class,
                () -> JsonParser.parse("{\"version\":\"1.2.1\"} invalid")
        );
    }

    @Test
    void rejectsLeadingZeroes() {
        assertThrows(IllegalArgumentException.class, () -> JsonParser.parse("01"));
    }
}
