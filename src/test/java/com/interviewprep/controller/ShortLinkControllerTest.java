package com.interviewprep.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewprep.dto.ShortenRequest;
import com.interviewprep.model.ShortLink;
import com.interviewprep.repository.ShortLinkRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser
class ShortLinkControllerTest {

    static final String LONG_URL = "https://example.com/articles/2026/10/a-very-long-path?ref=newsletter";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    ShortLinkRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void shortenReturnsCodeAndShortUrl() throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + LONG_URL + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(matchesPattern("[0-9A-Za-z]{1,8}")))
                .andExpect(jsonPath("$.shortUrl").value(matchesPattern("http://localhost/[0-9A-Za-z]{1,8}")))
                .andExpect(jsonPath("$.originalUrl").value(LONG_URL))
                .andExpect(header().exists("Location"));
    }

    @Test
    void redirectGoesToOriginalUrlAndEveryVisitIsCounted() throws Exception {
        String code = shorten(LONG_URL);

        for (int i = 0; i < 3; i++) {
            mvc.perform(get("/{code}", code))
                    .andExpect(status().isFound())
                    .andExpect(header().string("Location", LONG_URL));
        }

        mvc.perform(get("/api/urls/{code}/stats", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalUrl").value(LONG_URL))
                .andExpect(jsonPath("$.visitCount").value(3))
                .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void shorteningTheSameUrlTwiceCreatesTwoIndependentLinks() throws Exception {
        String first = shorten(LONG_URL);
        String second = shorten(LONG_URL);

        assertThat(first).isNotEqualTo(second);

        mvc.perform(get("/{code}", first)).andExpect(status().isFound());
        mvc.perform(get("/api/urls/{code}/stats", first)).andExpect(jsonPath("$.visitCount").value(1));
        mvc.perform(get("/api/urls/{code}/stats", second)).andExpect(jsonPath("$.visitCount").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not a url", "example.com", "ftp://example.com/file", "javascript:alert(1)", "https://"})
    void invalidUrlsAreRejected(String url) throws Exception {
        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ShortenRequest(url, null))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("url"));
    }

    @Test
    void expiryInThePastIsRejected() throws Exception {
        String body = "{\"url\":\"" + LONG_URL + "\",\"expiresAt\":\"2020-01-01T00:00:00Z\"}";

        mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("expiresAt"));
    }

    @Test
    void unknownCodeReturns404() throws Exception {
        mvc.perform(get("/nope123")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Short link nope123 not found"));
        mvc.perform(get("/api/urls/nope123/stats")).andExpect(status().isNotFound());
    }

    @Test
    void expiredCodeReturns410AndIsNotCounted() throws Exception {
        // Saved directly: the API refuses a past expiry, so this simulates a link whose time has run out.
        repository.save(new ShortLink("old1234", LONG_URL, Instant.now().minus(1, ChronoUnit.MINUTES)));

        mvc.perform(get("/old1234"))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.status").value(410));
        mvc.perform(get("/api/urls/old1234/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitCount").value(0));
    }

    @Test
    void concurrentVisitsAreAllCounted() throws Exception {
        String code = shorten(LONG_URL);
        int visitors = 50;
        ExecutorService pool = Executors.newFixedThreadPool(visitors);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < visitors; i++) {
                Callable<Integer> visit = () -> {
                    start.await();
                    return mvc.perform(get("/{code}", code)).andReturn().getResponse().getStatus();
                };
                results.add(pool.submit(visit));
            }
            start.countDown(); // release all visitors at once

            for (Future<Integer> result : results) {
                assertThat(result.get()).isEqualTo(302);
            }
        } finally {
            pool.shutdown();
        }

        mvc.perform(get("/api/urls/{code}/stats", code))
                .andExpect(jsonPath("$.visitCount").value(visitors));
    }

    private String shorten(String url) throws Exception {
        String response = mvc.perform(post("/api/urls").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"url\":\"" + url + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("code").asText();
    }
}
