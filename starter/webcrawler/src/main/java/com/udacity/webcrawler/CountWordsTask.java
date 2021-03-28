package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveAction;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CountWordsTask extends RecursiveAction {
    private final int maxDepth;
    private final Clock clock;
    private final Instant deadline;
    private final String url;
    private final PageParserFactory parserFactory;
    private final Map<String, Integer> counts;
    private final Set<String> visitedUrls;
    private final List<Pattern> ignoredUrls;

    public CountWordsTask(final int maxDepth, final Clock clock, final Instant deadline,
                          final String url, final PageParserFactory parserFactory,
                          final Map<String, Integer> counts, final Set<String> visitedUrls,
                          final List<Pattern> ignoredUrls) {
        this.maxDepth = maxDepth;
        this.clock = clock;
        this.deadline = deadline;
        this.url = url;
        this.parserFactory = parserFactory;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    protected void compute() {
        if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
            return;
        }

        for (final Pattern pattern : ignoredUrls) {
            if (pattern.matcher(url).matches()) {
                return;
            }
        }

        if (visitedUrls.contains(url)) {
            return;
        }

        visitedUrls.add(url);

        final PageParser.Result result = parserFactory.get(url).parse();
        countWordsInUrl(result, counts);

        final List<CountWordsTask> subTasks = result.getLinks()
                .stream()
                .map(link -> new CountWordsTask.Builder()
                        .setMaxDepth(maxDepth - 1)
                        .setClock(clock)
                        .setDeadline(deadline)
                        .setUrl(link)
                        .setParserFactory(parserFactory)
                        .setCounts(counts)
                        .setVisitedUrls(visitedUrls)
                        .setIgnoredUrls(ignoredUrls)
                        .build())
                .collect(Collectors.toList());

        invokeAll(subTasks);
    }

    public static void countWordsInUrl(final PageParser.Result result, final Map<String, Integer> counts) {
        for (final Map.Entry<String, Integer> e : result.getWordCounts().entrySet()) {
            counts.compute(e.getKey(), (k, v) -> (v == null) ? e.getValue() : counts.get(e.getKey()) + e.getValue());
        }
    }

    public static final class Builder {
        private int maxDepth;
        private Clock clock;
        private Instant deadline;
        private String url;
        private PageParserFactory parserFactory;
        private Map<String, Integer> counts;
        private Set<String> visitedUrls;
        private List<Pattern> ignoredUrls;

        public Builder setMaxDepth(final int maxDepth) {
            this.maxDepth = maxDepth;
            return this;
        }

        public Builder setClock(final Clock clock) {
            this.clock = clock;
            return this;
        }

        public Builder setDeadline(final Instant deadline) {
            this.deadline = deadline;
            return this;
        }

        public Builder setUrl(final String url) {
            this.url = url;
            return this;
        }

        public Builder setParserFactory(final PageParserFactory parserFactory) {
            this.parserFactory = parserFactory;
            return this;
        }

        public Builder setCounts(final Map<String, Integer> counts) {
            this.counts = counts;
            return this;
        }

        public Builder setVisitedUrls(final Set<String> visitedUrls) {
            this.visitedUrls = visitedUrls;
            return this;
        }

        public Builder setIgnoredUrls(final List<Pattern> ignoredUrls) {
            this.ignoredUrls = ignoredUrls;
            return this;
        }

        public CountWordsTask build() {
            return new CountWordsTask(maxDepth, clock, deadline, url,
                    parserFactory, counts, visitedUrls, ignoredUrls);
        }
    }
}
