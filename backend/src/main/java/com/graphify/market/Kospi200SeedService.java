package com.graphify.market;

import com.graphify.company.Company;
import com.graphify.company.CompanyRepository;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KOSPI 200 마스터 시드. 정적 리소스 {@code data/kospi200.csv}(ticker,name)를 SoT로
 * companies 테이블에 ticker 기준 UPSERT한다. 멱등 — 재실행 시 기존 행은 update만.
 * 항상 market="KOSPI", instrumentType="COMMON_STOCK", inKospi200=true 로 설정한다.
 */
@Service
public class Kospi200SeedService {

    private static final Logger log = LoggerFactory.getLogger(Kospi200SeedService.class);
    private static final String CSV_PATH = "data/kospi200.csv";

    private final CompanyRepository companyRepository;

    public Kospi200SeedService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    /** 시드 결과. total = inserted + updated. */
    public record SeedResult(int inserted, int updated, int total) {
    }

    @Transactional
    public SeedResult seed() {
        int inserted = 0;
        int updated = 0;
        for (String[] row : readCsv()) {
            String ticker = row[0];
            String name = row[1];
            Optional<Company> existing = companyRepository.findByTicker(ticker);
            if (existing.isPresent()) {
                applyFlags(existing.get(), name);
                companyRepository.save(existing.get());
                updated++;
            } else {
                Company company = new Company();
                company.setTicker(ticker);
                company.setName(name);
                applyFlags(company, name);
                companyRepository.save(company);
                inserted++;
            }
        }
        int total = inserted + updated;
        log.info("KOSPI 200 seed done: inserted={}, updated={}, total={}", inserted, updated, total);
        return new SeedResult(inserted, updated, total);
    }

    /** market/instrumentType/inKospi200 플래그를 항상 설정하고, 이름이 비어 있으면 채운다. */
    private void applyFlags(Company company, String name) {
        if (company.getName() == null || company.getName().isBlank()) {
            company.setName(name);
        }
        company.setMarket("KOSPI");
        company.setInstrumentType("COMMON_STOCK");
        company.setInKospi200(true);
        company.setUpdatedAt(Instant.now());
    }

    private java.util.List<String[]> readCsv() {
        java.util.List<String[]> rows = new java.util.ArrayList<>();
        try (InputStream in = new ClassPathResource(CSV_PATH).getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            boolean header = true;
            while ((line = reader.readLine()) != null) {
                if (header) {
                    header = false;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                int comma = line.indexOf(',');
                if (comma < 0) {
                    continue;
                }
                String ticker = line.substring(0, comma).trim();
                String name = line.substring(comma + 1).trim();
                if (ticker.isEmpty()) {
                    continue;
                }
                rows.add(new String[] {ticker, name});
            }
        } catch (IOException e) {
            throw new IllegalStateException("KOSPI 200 시드 CSV를 읽을 수 없습니다: " + CSV_PATH, e);
        }
        return rows;
    }
}
