package com.lsoftware.inventoco.infrastructure.ingest;

import com.lsoftware.inventoco.entity.catalog.gateway.CatalogSource;
import com.lsoftware.inventoco.usecase.catalog.IngestCatalogUseCase;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Refreshes the shared catalog and the store-chain prices of Colombia: D1, Éxito and Carulla
 * (VTEX catalog APIs, which their robots.txt allow) and Agro Fruver (its public inventory list).
 * Off unless INGEST_ENABLED=true. Runs daily on INGEST_CRON and, with INGEST_ON_START=true, once at
 * startup in the background. Olímpica is left out: its robots.txt disallows the query strings its
 * catalog API needs.
 */
@Slf4j
@Component
@EnableScheduling
@ConditionalOnProperty(name = "inventoco.ingest.enabled", havingValue = "true")
public class IngestJob {

    private final IngestCatalogUseCase ingest;
    private final Map<CatalogSource, List<String>> sources = new LinkedHashMap<>();
    private final int perTerm;
    private final boolean onStart;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public IngestJob(IngestCatalogUseCase ingest, RestClient.Builder builder,
                     @Value("${inventoco.ingest.per-term}") int perTerm,
                     @Value("${inventoco.ingest.pause}") Duration pause,
                     @Value("${inventoco.ingest.on-start}") boolean onStart,
                     @Value("${inventoco.ingest.d1.url}") String d1Url,
                     @Value("${inventoco.ingest.d1.terms}") String d1Terms,
                     @Value("${inventoco.ingest.exito.url}") String exitoUrl,
                     @Value("${inventoco.ingest.carulla.url}") String carullaUrl,
                     @Value("${inventoco.ingest.search-terms}") String searchTerms,
                     @Value("${inventoco.ingest.agrofruver.url}") String agroUrl) {
        this.ingest = ingest;
        this.perTerm = perTerm;
        this.onStart = onStart;
        long ms = pause.toMillis();
        sources.put(new VtexCatalogSource("D1", "CO", "COP", d1Url, builder.clone(), ms), split(d1Terms));
        sources.put(new VtexCatalogSource("Éxito", "CO", "COP", exitoUrl, builder.clone(), ms), split(searchTerms));
        sources.put(new VtexCatalogSource("Carulla", "CO", "COP", carullaUrl, builder.clone(), ms), split(searchTerms));
        sources.put(new AgroFruverCatalogSource(agroUrl, builder.clone()), List.of("*"));
    }

    @EventListener(ApplicationReadyEvent.class)
    public void atStart() {
        if (onStart) Thread.ofVirtual().name("catalog-ingest").start(this::run);
    }

    @Scheduled(cron = "${inventoco.ingest.cron}")
    public void run() {
        if (!running.compareAndSet(false, true)) return;
        try {
            sources.forEach((source, terms) -> ingest.run(source, terms, perTerm));
        } catch (RuntimeException e) {
            log.warn("Catalog ingest stopped: {}", e.getMessage());
        } finally {
            running.set(false);
        }
    }

    private static List<String> split(String csv) {
        return Arrays.stream(csv.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }
}
