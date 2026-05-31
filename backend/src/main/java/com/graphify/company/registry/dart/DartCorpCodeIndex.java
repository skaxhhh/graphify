package com.graphify.company.registry.dart;

import com.graphify.config.GraphifyDartProperties;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

@Component
public class DartCorpCodeIndex {

    private static final Logger log = LoggerFactory.getLogger(DartCorpCodeIndex.class);
    private static final String CORP_CODE_URL = "https://opendart.fss.or.kr/api/corpCode.xml";

    private final GraphifyDartProperties dartProperties;
    private final RestClient dartRestClient;

    private volatile List<DartCorpCodeEntry> entries = List.of();
    private volatile Instant loadedAt = Instant.EPOCH;

    public DartCorpCodeIndex(GraphifyDartProperties dartProperties, RestClient dartRestClient) {
        this.dartProperties = dartProperties;
        this.dartRestClient = dartRestClient;
    }

    public List<DartCorpCodeEntry> search(String query, int limit) {
        if (!dartProperties.hasApiKey()) {
            return List.of();
        }
        ensureLoaded();
        String normalized = normalize(query);
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<ScoredEntry> scored = new ArrayList<>();
        for (DartCorpCodeEntry entry : entries) {
            int score = scoreEntry(entry, normalized);
            if (score > 0) {
                scored.add(new ScoredEntry(entry, score));
            }
        }

        scored.sort(Comparator
                .comparingInt(ScoredEntry::score).reversed()
                .thenComparing((ScoredEntry s) -> !s.entry().isListed())
                .thenComparing(s -> s.entry().corpName()));

        return scored.stream()
                .limit(Math.max(1, limit))
                .map(ScoredEntry::entry)
                .toList();
    }

    public synchronized void ensureLoaded() {
        if (!dartProperties.hasApiKey()) {
            return;
        }
        int cacheHours = Math.max(1, dartProperties.getCorpCodeCacheHours());
        boolean stale = Duration.between(loadedAt, Instant.now()).toHours() >= cacheHours;
        if (!entries.isEmpty() && !stale) {
            return;
        }
        try {
            entries = loadFromDart();
            loadedAt = Instant.now();
            log.info("DART corpCode 인덱스 로드 완료: {}건", entries.size());
        } catch (Exception ex) {
            log.warn("DART corpCode 로드 실패: {}", ex.getMessage());
            if (entries.isEmpty()) {
                entries = List.of();
            }
        }
    }

    private List<DartCorpCodeEntry> loadFromDart() throws Exception {
        byte[] zipBytes = dartRestClient.get()
                .uri(CORP_CODE_URL + "?crtfc_key={key}", dartProperties.getApiKey().trim())
                .retrieve()
                .body(byte[].class);

        if (zipBytes == null || zipBytes.length == 0) {
            return List.of();
        }

        byte[] xmlBytes = extractCorpCodeXml(zipBytes);
        return parseCorpCodeXml(xmlBytes);
    }

    private static byte[] extractCorpCodeXml(byte[] zipBytes) throws Exception {
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().toUpperCase(Locale.ROOT).endsWith(".XML")) {
                    return zip.readAllBytes();
                }
            }
        }
        throw new IllegalStateException("CORPCODE.xml not found in zip");
    }

    private static List<DartCorpCodeEntry> parseCorpCodeXml(byte[] xmlBytes) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xmlBytes));
        NodeList listNodes = document.getElementsByTagName("list");
        List<DartCorpCodeEntry> parsed = new ArrayList<>(listNodes.getLength());
        for (int i = 0; i < listNodes.getLength(); i++) {
            Element element = (Element) listNodes.item(i);
            String corpCode = textContent(element, "corp_code");
            String corpName = textContent(element, "corp_name");
            if (corpCode == null || corpName == null) {
                continue;
            }
            parsed.add(new DartCorpCodeEntry(
                    corpCode.trim(),
                    corpName.trim(),
                    textContent(element, "stock_code"),
                    textContent(element, "modify_date")
            ));
        }
        return parsed;
    }

    private static String textContent(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0 || nodes.item(0) == null) {
            return null;
        }
        String value = nodes.item(0).getTextContent();
        return value == null ? null : value.trim();
    }

    private static int scoreEntry(DartCorpCodeEntry entry, String query) {
        String name = normalize(entry.corpName());
        String ticker = entry.stockCode() == null ? "" : normalize(entry.stockCode());

        if (name.equals(query) || ticker.equals(query)) {
            return 100;
        }
        if (name.startsWith(query) || ticker.startsWith(query)) {
            return 80;
        }
        if (name.contains(query) || ticker.contains(query)) {
            return 50;
        }
        return 0;
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace(" ", "");
    }

    private record ScoredEntry(DartCorpCodeEntry entry, int score) {}
}
