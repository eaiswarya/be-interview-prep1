package com.interviewprep.controller;

import com.interviewprep.dto.ShortLinkResponse;
import com.interviewprep.dto.ShortLinkStatsResponse;
import com.interviewprep.dto.ShortenRequest;
import com.interviewprep.model.ShortLink;
import com.interviewprep.service.ShortLinkService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
public class ShortLinkController {

    private final ShortLinkService service;

    public ShortLinkController(ShortLinkService service) {
        this.service = service;
    }

    @PostMapping("/api/urls")
    public ResponseEntity<ShortLinkResponse> shorten(@Valid @RequestBody ShortenRequest request) {
        ShortLink link = service.shorten(request.url(), request.expiresAt());
        String shortUrl = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/{code}").buildAndExpand(link.getCode()).toUriString();
        return ResponseEntity.created(URI.create(shortUrl)).body(ShortLinkResponse.from(link, shortUrl));
    }

    /**
     * 302 (temporary) rather than 301: browsers cache a 301 and stop asking the server, so later visits
     * would not be counted. The regex limits this catch-all path to valid codes, so /favicon.ico or /h2-console never reach it.
     */
    @GetMapping("/{code:[0-9A-Za-z]{1,8}}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(service.visit(code))).build();
    }

    @GetMapping("/api/urls/{code}/stats")
    public ShortLinkStatsResponse stats(@PathVariable String code) {
        return ShortLinkStatsResponse.from(service.stats(code));
    }
}
