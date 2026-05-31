export interface AutocompleteItem {
  id: number;
  name: string;
  ticker: string | null;
  matchType: string;
}

export interface TermItem {
  id: number;
  type: string;
  title: string;
  version: string;
  required: boolean;
  content: string | null;
}

export interface TermsLatest {
  version: string;
  terms: TermItem[];
  companyCount: number;
}

export interface RecentSearchEntry {
  companyId: number;
  label: string;
  searchedAt: string;
}

export type CompanySort = "name" | "industry" | "updatedAt";

export type CompanySearchSource = "LOCAL" | "EXTERNAL";

export interface CompanySearchItem {
  id: number;
  name: string;
  ticker: string | null;
  industry: string | null;
  market: string | null;
  dataFreshness: string;
  updatedAt: string;
  source?: CompanySearchSource;
  syncStatus?: string;
}

export interface SimilarCompany {
  id: number;
  name: string;
  ticker: string | null;
}

export interface SemanticHints {
  relatedQueries: string[];
  similarCompanies: SimilarCompany[];
}

export interface CompanySearchData {
  items: CompanySearchItem[];
  semanticHints: SemanticHints;
}

export interface CompanySearchParams {
  q: string;
  sort?: CompanySort;
  industry?: string;
  market?: string;
  dataStatus?: string;
  page?: number;
  size?: number;
  enrich?: boolean;
  enrichThreshold?: number;
}

export interface CompanyResolveResult {
  id: number;
  name: string;
  ticker: string | null;
  syncStatus: string;
  created: boolean;
}
