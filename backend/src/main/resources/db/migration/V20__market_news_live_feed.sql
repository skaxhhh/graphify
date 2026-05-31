ALTER TABLE market_news
    ADD COLUMN IF NOT EXISTS feed_source VARCHAR(32);

DELETE FROM market_news;

CREATE UNIQUE INDEX IF NOT EXISTS uk_market_news_source_url
    ON market_news (source_url)
    WHERE source_url IS NOT NULL AND source_url <> '';
