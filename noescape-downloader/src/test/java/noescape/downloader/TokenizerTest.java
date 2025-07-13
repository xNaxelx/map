package noescape.downloader;

import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class TokenizerTest {

    @Test
    void testEmpty() {
        var tokenizer = new Tokenizer("");
        assertFalse(tokenizer.hasNext());
        assertThrows(NoSuchElementException.class, tokenizer::next);
    }

    @Test
    void testBlank() {
        var tokenizer = new Tokenizer(" \t\n\r");
        assertFalse(tokenizer.hasNext());
        assertThrows(NoSuchElementException.class, tokenizer::next);
    }

    @Test
    void testSingleWord() {
        var tokenizer = new Tokenizer("foo");
        assertTrue(tokenizer.hasNext());
        assertEquals("foo", tokenizer.next());
    }

    @Test
    void testSingleIntNumber() {
        var tokenizer = new Tokenizer("42");
        assertTrue(tokenizer.hasNext());
        assertEquals("42", tokenizer.next());
    }

    @Test
    void testTrailingWhitespace() {
        var tokenizer = new Tokenizer("foo  ");
        assertEquals("foo", tokenizer.next());
        assertFalse(tokenizer.hasNext());
    }

    @Test
    void testMultipleWords() {
        var tokenizer = new Tokenizer("foo bar \n buzz");
        assertEquals("foo", tokenizer.next());
        assertEquals("bar", tokenizer.next());
        assertEquals("buzz", tokenizer.next());
        assertFalse(tokenizer.hasNext());
    }

    @Test
    void testMixedLettersAndDigits() {
        var tokenizer = new Tokenizer("foo42 777bar ОСОБА_1");
        assertEquals("foo", tokenizer.next());
        assertEquals("42", tokenizer.next());
        assertEquals("777", tokenizer.next());
        assertEquals("bar", tokenizer.next());
        assertEquals("ОСОБА_1", tokenizer.next());
        assertFalse(tokenizer.hasNext());
    }

    @Test
    void testPunct() {
        var tokenizer = new Tokenizer(".,;_(«");
        assertEquals(".", tokenizer.next());
        assertEquals(",", tokenizer.next());
        assertEquals(";", tokenizer.next());
        assertEquals("_", tokenizer.next());
        assertEquals("(", tokenizer.next());
        assertEquals("«", tokenizer.next());
        assertFalse(tokenizer.hasNext());
    }

    @Test
    void testDateAndTime() {
        var tokenizer = new Tokenizer("00.00 00:00 31.12.2024 31/12/2024");
        assertEquals("00.00", tokenizer.next());
        assertEquals("00:00", tokenizer.next());
        assertEquals("31.12.2024", tokenizer.next());
        assertEquals("31/12/2024", tokenizer.next());
        assertFalse(tokenizer.hasNext());
    }

    @Test
    void testSentence() {
        var tokenizer = new Tokenizer("Hello, Lorem ipsum. Bye!");
        assertEquals("Hello", tokenizer.next());
        assertEquals(",", tokenizer.next());
        assertEquals("Lorem", tokenizer.next());
        assertEquals("ipsum", tokenizer.next());
        assertEquals(".", tokenizer.next());
        assertEquals("Bye", tokenizer.next());
        assertEquals("!", tokenizer.next());
        assertFalse(tokenizer.hasNext());
    }

    @Test
    void testUkrainianAlphabet() {
        var tokenizer = new Tokenizer("обʼєднаної територіальної");
        assertEquals("обʼєднаної", tokenizer.next());
        assertEquals("територіальної", tokenizer.next());
        assertFalse(tokenizer.hasNext());
    }

    @Test
    void testAbbreviation() {
        var tokenizer = new Tokenizer("ч.2 ст.204");
        assertEquals("ч", tokenizer.next());
        assertEquals(".", tokenizer.next());
        assertEquals("2", tokenizer.next());
        assertEquals("ст", tokenizer.next());
        assertEquals(".", tokenizer.next());
        assertEquals("204", tokenizer.next());
        assertFalse(tokenizer.hasNext());
    }
}