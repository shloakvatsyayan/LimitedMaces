package org.bonkmc.limitedmaces.updates.json;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class JsonStringParserTest {
    @Test
    void parsesEscapedStringAndReportsFollowingPosition() {
        JsonStringParser stringParser = new JsonStringParser("\"Limited\\nMaces\" next", 0);

        assertEquals("Limited\nMaces", stringParser.parse());
        assertEquals(16, stringParser.position());
    }

    @Test
    void rejectsInvalidEscapeSequence() {
        JsonStringParser stringParser = new JsonStringParser("\"invalid\\x\"", 0);

        assertThrows(IllegalArgumentException.class, stringParser::parse);
    }
}
