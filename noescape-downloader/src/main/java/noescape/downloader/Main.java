package noescape.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Random;

import static noescape.downloader.CourtRegion.*;
import static noescape.downloader.TextCleaner.cleanText;
import static noescape.downloader.TextExtractor.extractText;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);
    private static final int BATCH_SIZE = 99;

    public static void main(String[] args) throws InterruptedException, IOException {
        var applicationProperties = new ApplicationConfig();
        var htmlDir = Path.of(applicationProperties.getProperty("html.dir", "html"));
        var textDir = Path.of(applicationProperties.getProperty("text.dir", "text"));
        var htmlRepository = new HtmlRepository(htmlDir);
        var textRepository = new TextRepository(textDir);
        var downloader = new Downloader(htmlRepository);

        LocalDate today = LocalDate.now();
        for (int i = 0; i <= new Random().nextInt(4); i++) {
            var endDate = today.minusMonths(i);
            var startDate = endDate.minusMonths(1);

            CourtRegion[] regions = new CourtRegion[]{
                    ZAKARPAT,
                    ODESA,
                    CHERNIVCI,
                    VINNITZA,
                    IVANO_FRANKIVSK,
                    VOLYN,
                    LVIV,
                    RIVNE,
                    TERNOPIL,
                    KHMELNITSK,
                    ZHYTOMIR,
                    KYIVSKA,
                    CHERNYGIV,
                    SUMY};
            log.info("Downloading {} regions starting from {} to {}. Storing files to {}", regions.length, startDate, endDate, htmlDir.toAbsolutePath());
            downloader.downloadBatch(BATCH_SIZE, startDate.toString(), endDate.toString(), regions);
            log.info("Download complete");
        }

        var newCaseIds = htmlRepository.getCaseIds().stream()
                .filter(caseId -> !textRepository.fileExists(caseId))
                .toList();

        log.info("Extracting {} texts to {}", newCaseIds.size(), textDir.toAbsolutePath());
        newCaseIds.parallelStream().forEach(caseId -> extracted(caseId, htmlRepository, textRepository));
        log.info("Text extraction complete");
    }

    private static void extracted(String caseId, HtmlRepository htmlRepository, TextRepository textRepository) {
        var html = htmlRepository.getHtml(caseId);
        var text = extractText(html);
        text = cleanText(text);
        textRepository.saveText(caseId, text);
    }
}
