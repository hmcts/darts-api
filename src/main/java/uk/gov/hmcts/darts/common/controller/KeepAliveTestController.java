package uk.gov.hmcts.darts.common.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@ConditionalOnProperty(prefix = "darts", name = "api-pod", havingValue = "true")
@SuppressWarnings("PMD")
public class KeepAliveTestController {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @GetMapping(value = "/keep-alive-test-stream", produces = MediaType.APPLICATION_STREAM_JSON_VALUE)
    public Flux<MyData> streamLongTaskSingleResult() {
        AtomicInteger counter = new AtomicInteger(0);

        Mono<MyData> longTask = Mono.fromFuture(CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(45000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return new MyData("Final Data", 100);
        }));

        Flux<MyData> interimData = Flux.interval(Duration.ofSeconds(1))
            .map(i -> new MyData("Interim Data " + i, counter.incrementAndGet() * 5))
            .takeUntilOther(longTask)
            .onBackpressureDrop();

        return Flux.concat(interimData, longTask).delayElements(Duration.ofMillis(500));
    }

    @GetMapping("/keep-alive-test")
    public Flux<ServerSentEvent<String>> keepAliveTest() {
        Mono<ServerSentEvent<String>> longSearchSimulation = Mono.fromCallable(() -> {
                try {
                    Thread.sleep(45000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                List<String> data = List.of("Result 1", "Result 2", "Result 3");
                try {
                    String jsonData = objectMapper.writeValueAsString(data);
                    return ServerSentEvent.<String>builder()
                        .event("long-search-complete")
                        .data(jsonData)
                        .build();
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    return ServerSentEvent.<String>builder()
                        .event("long-search-complete")
                        .data("Error serializing data to JSON")
                        .build();
                }


            }).delaySubscription(Duration.ofSeconds(1));

        Flux<ServerSentEvent<String>> periodicEvents = Flux.interval(Duration.ofMillis(500))
            .takeUntilOther(longSearchSimulation)
            .map(sequence -> {
                Map<String, Object> periodicData = Map.of("time", LocalTime.now().toString(), "sequence", sequence);
                try {
                    String jsonData = objectMapper.writeValueAsString(periodicData);
                    return ServerSentEvent.<String>builder()
                        .id(String.valueOf(sequence))
                        .event("keep-alive")
                        .data(jsonData)
                        .build();
                } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
                    return ServerSentEvent.<String>builder()
                        .id(String.valueOf(sequence))
                        .event("keep-alive")
                        .data("Error serializing periodic data to JSON")
                        .build();
                }
            });

        return Flux.merge(periodicEvents, longSearchSimulation);
    }

    static class MyData {
        private String name;
        private int value;

        public MyData(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public int getValue() {
            return value;
        }
    }
}
