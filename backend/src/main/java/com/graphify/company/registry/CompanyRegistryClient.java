package com.graphify.company.registry;

import java.util.List;
import java.util.Optional;

public interface CompanyRegistryClient {

    List<ExternalCompanyCandidate> searchByKeyword(String query, int limit);

    Optional<ExternalCompanyProfile> findByTicker(String ticker);

    Optional<ExternalCompanyProfile> findByExternalId(String source, String externalId);
}
