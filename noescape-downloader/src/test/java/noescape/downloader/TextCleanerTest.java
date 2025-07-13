package noescape.downloader;

import org.junit.jupiter.api.Test;

import static noescape.downloader.TextCleaner.fixQuotes;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TextCleanerTest {

    @Test
    void testFixQuotes() {
        assertEquals("test", fixQuotes("test"));
        assertEquals("«test»", fixQuotes("'test'"));
        assertEquals("lorem «ipsum»", fixQuotes("lorem 'ipsum'"));
        assertEquals("lorem «ipsum»", fixQuotes("lorem «ipsum'"));
        assertEquals("lorem «ipsum»,", fixQuotes("lorem «ipsum',"));
        assertEquals("«ipsum»", fixQuotes("'ipsum»"));
        assertEquals("«ipsum»", fixQuotes("\"ipsum»"));
        assertEquals("«ipsum»", fixQuotes("\"ipsum»"));
        assertEquals("«ipsum»", fixQuotes("„ipsum»"));
        assertEquals("«Про державний кордон України»", fixQuotes("\"Про державний кордон України\""));
        assertEquals("відділу прикордонної служби «ІНФОРМАЦІЯ_1» про притягнення",
                fixQuotes("відділу прикордонної служби \" ІНФОРМАЦІЯ_1 \" про притягнення"));
        assertEquals("Законом України «Про затвердження Указу Президента України \"Про введення надзвичайного стану в окремих регіонах України\"» від 23 лютого 2022 року № 2101-IX, затверджено Указ Президента України від 23 лютого 2022 року № 63/2022 «Про введення надзвичайного стану в окремих регіонах України».",
                fixQuotes("Законом України «Про затвердження Указу Президента України \"Про введення надзвичайного стану в окремих регіонах України\"» від 23 лютого 2022 року № 2101-IX, затверджено Указ Президента України від 23 лютого 2022 року № 63/2022 «Про введення надзвичайного стану в окремих регіонах України»."));
    }
}