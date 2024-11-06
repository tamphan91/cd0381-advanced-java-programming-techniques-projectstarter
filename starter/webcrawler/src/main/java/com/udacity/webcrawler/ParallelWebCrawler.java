package com.udacity.webcrawler;

import com.udacity.webcrawler.json.CrawlResult;
import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import javax.inject.Inject;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

/**
 * A concrete implementation of {@link WebCrawler} that runs multiple threads on a
 * {@link ForkJoinPool} to fetch and process multiple web pages in parallel.
 */
final class ParallelWebCrawler implements WebCrawler {
    private final Clock clock;
    private final Duration timeout;
    private final int popularWordCount;
    private final ForkJoinPool pool;
    private final PageParserFactory pageParserFactory;
    private final List<Pattern> ignoredUrls;
    private final int maxDepth;

    @Inject
    ParallelWebCrawler(
            Clock clock,
            @Timeout Duration timeout,
            @PopularWordCount int popularWordCount,
            @TargetParallelism int threadCount,
            PageParserFactory pageParserFactory,
            @IgnoredUrls List<Pattern> ignoredUrls,
            @MaxDepth int maxDepth) {
        this.clock = clock;
        this.timeout = timeout;
        this.popularWordCount = popularWordCount;
        this.pool = new ForkJoinPool(Math.min(threadCount, getMaxParallelism()));
        this.pageParserFactory = pageParserFactory;
        this.ignoredUrls = ignoredUrls;
        this.maxDepth = maxDepth;
    }

    @Override
    public CrawlResult crawl(List<String> startingUrls) {
        Instant deadline = clock.instant().plus(timeout);
        ConcurrentMap<String, Integer> counts = new ConcurrentHashMap<>();
        ConcurrentSkipListSet<String> visitedUrls = new ConcurrentSkipListSet<>();
        for (String url : startingUrls) {
            pool.invoke(new InternalCrawler(url, deadline, maxDepth, counts, visitedUrls));
        }

        if (counts.isEmpty()) {
            return new CrawlResult.Builder()
                    .setWordCounts(counts)
                    .setUrlsVisited(visitedUrls.size())
                    .build();
        }

        return new CrawlResult.Builder()
                .setWordCounts(WordCounts.sort(counts, popularWordCount))
                .setUrlsVisited(visitedUrls.size())
                .build();
    }

    public class InternalCrawler extends RecursiveTask<Boolean> {
        private final String url;
        private final Instant deadline;
        private final int maxDepth;
        private final ConcurrentMap<String, Integer> counts;
        private final ConcurrentSkipListSet<String> visitedUrls;

        public InternalCrawler(String url,
                               Instant deadline,
                               int maxDepth,
                               ConcurrentMap<String,
                                       Integer> counts,
                               ConcurrentSkipListSet<String> visitedUrls) {
            this.url = url;
            this.deadline = deadline;
            this.maxDepth = maxDepth;
            this.counts = counts;
            this.visitedUrls = visitedUrls;
        }

        @Override
        protected Boolean compute() {
            if (maxDepth == 0 || clock.instant().isAfter(deadline)) {
                return false;
            }
            for (Pattern pattern : ignoredUrls) {
                if (pattern.matcher(url).matches()) {
                    return false;
                }
            }

            synchronized (this) {
                if (visitedUrls.contains(url) || !visitedUrls.add(url)) {
                    return false;
                }
            }

            PageParser.Result result = pageParserFactory.get(url).parse();
            for (ConcurrentMap.Entry<String, Integer> entry : result.getWordCounts().entrySet()) {
                counts.compute(entry.getKey(), (k, v) -> (v == null) ? entry.getValue() : entry.getValue() + v);
            }
            List<InternalCrawler> subtasks = new ArrayList<>();
            for (String link : result.getLinks()) {
                subtasks.add(new InternalCrawler(link, deadline, maxDepth - 1, counts, visitedUrls));
            }
            invokeAll(subtasks);
            return true;
        }
    }

    @Override
    public int getMaxParallelism() {
        return Runtime.getRuntime().availableProcessors();
    }
}
