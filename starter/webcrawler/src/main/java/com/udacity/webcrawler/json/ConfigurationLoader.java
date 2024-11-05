package com.udacity.webcrawler.json;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A static utility class that loads a JSON configuration file.
 */
public final class ConfigurationLoader {

    private final Path path;

    /**
     * Create a {@link ConfigurationLoader} that loads configuration from the given {@link Path}.
     */
    public ConfigurationLoader(Path path) {
        this.path = Objects.requireNonNull(path);
    }

    /**
     * Loads configuration from this {@link ConfigurationLoader}'s path
     *
     * @return the loaded {@link CrawlerConfiguration}.
     */
    public CrawlerConfiguration load() {
        // TODO: Fill in this method.

        return new CrawlerConfiguration.Builder().build();
    }

    /**
     * Loads crawler configuration from the given reader.
     *
     * @param reader a Reader pointing to a JSON string that contains crawler configuration.
     * @return a crawler configuration
     */
    public static CrawlerConfiguration read(Reader reader) throws IOException {
        // This is here to get rid of the unused variable warning.
        Objects.requireNonNull(reader);
        JsonFactory jsonFactory = new JsonFactory();
        jsonFactory.configure(JsonGenerator.Feature.AUTO_CLOSE_TARGET, false);
        jsonFactory.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, false);
        ObjectMapper objectMapper = new ObjectMapper(jsonFactory);
        CrawlerConfiguration crawlerConfiguration = objectMapper.readValue(reader, CrawlerConfiguration.class);
        return new CrawlerConfiguration.Builder()
                .addStartPages(crawlerConfiguration.getStartPages().toArray(new String[0]))
                .addIgnoredUrls(crawlerConfiguration.getIgnoredUrls().stream().map(Objects::toString).toList().toArray(new String[0]))
                .addIgnoredWords(crawlerConfiguration.getIgnoredWords().stream().map(Objects::toString).toList().toArray(new String[0]))
                .setParallelism(crawlerConfiguration.getParallelism())
                .setImplementationOverride(crawlerConfiguration.getImplementationOverride())
                .setMaxDepth(crawlerConfiguration.getMaxDepth())
                .setTimeoutSeconds((int) crawlerConfiguration.getTimeout().getSeconds())
                .setPopularWordCount(crawlerConfiguration.getPopularWordCount())
                .setProfileOutputPath(crawlerConfiguration.getProfileOutputPath())
                .setResultPath(crawlerConfiguration.getResultPath())
                .build();
    }
}
