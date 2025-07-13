package noescape.downloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Map;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.net.http.HttpResponse.BodyHandlers;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Map.entry;
import static java.util.Map.ofEntries;
import static org.apache.commons.lang3.StringUtils.substringBetween;

public class Downloader {

    private static final Logger log = LoggerFactory.getLogger(Downloader.class);
    private final URI uri = URI.create("https://reyestr.court.gov.ua/");
    private final HttpClient httpClient = HttpClient.newBuilder().build();
    private String cookie;

    private final HtmlRepository htmlRepository;
    private final Random random = new Random();

    public Downloader(HtmlRepository htmlRepository) {
        this.htmlRepository = htmlRepository;
    }

    public void downloadAll(String startDate) throws IOException, InterruptedException {
        downloadBatch(Integer.MAX_VALUE, startDate, "", CourtRegion.values());
    }

    public void downloadBatch(int batchSize, String startDate, String endDate, CourtRegion... courtRegions) throws IOException, InterruptedException {
        int count = 0;
        outer: for (var courtRegion : courtRegions) {
            log.info("Searching court cases in {}", courtRegion);
            String html = search(courtRegion, startDate, endDate);
            log.info("Getting court case numbers for {}", courtRegion);
            int pageCount = getPageCount(html);
            log.info("Scanning {} pages", pageCount);
            var pattern = Pattern.compile("href=\"/Review/(\\d+)\"");
            for (int page = 1; page <= pageCount; page++) {
                Thread.sleep(500 + random.nextInt(5_000)); // Do not overload the server with requests
                log.info("Downloading page {}", page);
                String pageBody = getPage(page);
                var matcher = pattern.matcher(pageBody);
                while (matcher.find()) {
                    String caseId = matcher.group(1);
                    if (!htmlRepository.exists(caseId)) {
                        Thread.sleep(500 + random.nextInt(5_000)); // Do not overload the server with requests
                        log.info("Downloading case {}", caseId);
                        String caseBody = getCase(caseId);
                        htmlRepository.saveHtml(caseId, caseBody);
                        count++;
                        if (count >= batchSize) break outer;
                    }
                }
            }
        }
        log.info("Batch downloaded: {}", count);
    }

    private String search(CourtRegion courtRegion, String startDate, String endDate) throws IOException, InterruptedException {
        Map<String, String> formData = ofEntries(
                entry("SearchExpression", ""),
                entry("CourtRegion", courtRegion.code),
                entry("UserCourtCode", ""),
                entry("ChairmenName", ""),
                entry("RegNumber", ""),
                entry("RegDateBegin", ""),
                entry("RegDateEnd", ""),
                entry("ImportDateBegin", startDate),
                entry("ImportDateEnd", endDate),
                entry("CSType", "5"),
                entry("CaseCat1", "40933"),
                entry("CaseCat2", "41257"),
                entry("CaseCat3", "41352"),
                entry("CaseId", ""),
                entry("Sort", ""),
                entry("PagingInfo.ItemsPerPage", "125"),
                entry("Liga", "true"),
                entry("ProviderItem", "1")
        );

        var httpRequest = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(getFormDataAsString(formData)))
                .build();
        var response = httpClient.send(httpRequest, BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new RuntimeException("search " + " " + response.statusCode());
        cookie = response.headers().firstValue("set-cookie").get();
        return response.body();
    }

    private String getPage(int page) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(uri + "Page/" + page))
                .GET()
                .headers("Cookie", cookie)
                .build();
        var response = httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new RuntimeException("page " + page + " " + response.statusCode());
        return response.body();
    }

    private String getCase(String caseId) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(URI.create(uri + "Review/" + caseId))
                .GET()
                .headers("Cookie", cookie)
                .build();
        var response = httpClient.send(request, BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new RuntimeException("case " + caseId + " " + response.statusCode());
        return response.body();
    }

    private static int getPageCount(String html) {
        String ti = substringBetween(html, "var ti = ", ";");
        return (int) Math.ceil(Integer.parseInt(ti) / 100.0);
    }

    private String getFormDataAsString(Map<String, String> formData) {
        return formData.entrySet().stream()
                .map(entry -> URLEncoder.encode(entry.getKey(), UTF_8) + "=" + URLEncoder.encode(entry.getValue(), UTF_8))
                .collect(Collectors.joining("&"));
    }

}
