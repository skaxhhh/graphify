package com.graphify.company;

import com.graphify.company.registry.ExternalCompanyCandidate;
import com.graphify.company.registry.ExternalCompanyProfile;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CompanyUpsertService {

    private final CompanyRepository companyRepository;

    public CompanyUpsertService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompanyUpsertResult upsertFromCandidate(ExternalCompanyCandidate candidate) {
        Optional<Company> existing = findExisting(candidate.externalSource(), candidate.externalId(), candidate.tickerOptional().orElse(null), candidate.name());
        if (existing.isPresent()) {
            return new CompanyUpsertResult(touchExisting(existing.get(), candidate), false);
        }
        return new CompanyUpsertResult(insertStub(candidate), true);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Company upsertFromProfile(ExternalCompanyProfile profile) {
        Optional<Company> existing = findExisting(
                profile.externalSource(),
                profile.externalId(),
                profile.ticker(),
                profile.name()
        );
        Company company = existing.orElseGet(() -> insertFromProfile(profile));
        applyProfile(company, profile);
        company.setSyncStatus("PARTIAL");
        company.setDataStatus("STALE");
        company.setDetailSyncedAt(Instant.now());
        company.setUpdatedAt(Instant.now());
        return companyRepository.save(company);
    }

    private Optional<Company> findExisting(String source, String externalId, String ticker, String name) {
        if (ticker != null && !ticker.isBlank()) {
            Optional<Company> byTicker = companyRepository.findByTicker(ticker.trim());
            if (byTicker.isPresent()) {
                return byTicker;
            }
        }
        if (source != null && externalId != null && !externalId.isBlank()) {
            Optional<Company> byExternal = companyRepository.findByExternalSourceAndExternalId(source, externalId);
            if (byExternal.isPresent()) {
                return byExternal;
            }
        }
        if (name != null && !name.isBlank()) {
            return companyRepository.findFirstByNameIgnoreCase(name.trim());
        }
        return Optional.empty();
    }

    private Company touchExisting(Company company, ExternalCompanyCandidate candidate) {
        if (company.getExternalSource() == null && candidate.externalSource() != null) {
            company.setExternalSource(candidate.externalSource());
        }
        if (company.getExternalId() == null && candidate.externalId() != null) {
            company.setExternalId(candidate.externalId());
        }
        if (company.getTicker() == null && candidate.tickerOptional().isPresent()) {
            company.setTicker(candidate.tickerOptional().get());
        }
        if (company.getListed() == null) {
            company.setListed(candidate.listed());
        }
        company.setUpdatedAt(Instant.now());
        return companyRepository.save(company);
    }

    private Company insertStub(ExternalCompanyCandidate candidate) {
        Company company = new Company();
        company.setName(candidate.name());
        candidate.tickerOptional().ifPresent(company::setTicker);
        company.setMarket(candidate.market());
        company.setExternalSource(candidate.externalSource());
        company.setExternalId(candidate.externalId());
        company.setListed(candidate.listed());
        company.setSyncStatus("STUB");
        company.setDataStatus("STALE");
        company.setSummary("");
        company.setUpdatedAt(Instant.now());
        return companyRepository.save(company);
    }

    private Company insertFromProfile(ExternalCompanyProfile profile) {
        Company company = new Company();
        company.setName(profile.name());
        company.setTicker(emptyToNull(profile.ticker()));
        company.setIndustry(emptyToNull(profile.industry()));
        company.setMarket(emptyToNull(profile.market()));
        company.setSummary(profile.summary() != null ? profile.summary() : "");
        company.setExternalSource(profile.externalSource());
        company.setExternalId(profile.externalId());
        company.setListed(profile.listed());
        company.setSyncStatus("PARTIAL");
        company.setDataStatus("STALE");
        company.setDetailSyncedAt(Instant.now());
        company.setUpdatedAt(Instant.now());
        return companyRepository.save(company);
    }

    private static void applyProfile(Company company, ExternalCompanyProfile profile) {
        company.setName(profile.name());
        if (profile.ticker() != null && !profile.ticker().isBlank()) {
            company.setTicker(profile.ticker().trim());
        }
        if (profile.industry() != null && !profile.industry().isBlank()) {
            company.setIndustry(profile.industry().trim());
        }
        if (profile.market() != null && !profile.market().isBlank()) {
            company.setMarket(profile.market().trim());
        }
        if (profile.summary() != null && !profile.summary().isBlank()) {
            company.setSummary(profile.summary().trim());
        }
        company.setListed(profile.listed());
        if (company.getExternalSource() == null) {
            company.setExternalSource(profile.externalSource());
        }
        if (company.getExternalId() == null) {
            company.setExternalId(profile.externalId());
        }
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
