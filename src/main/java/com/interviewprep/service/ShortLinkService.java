package com.interviewprep.service;

import com.interviewprep.exception.ResourceGoneException;
import com.interviewprep.exception.ResourceNotFoundException;
import com.interviewprep.model.ShortLink;
import com.interviewprep.repository.ShortLinkRepository;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortLinkService {

    static final int MAX_CODE_ATTEMPTS = 5;

    private final ShortLinkRepository repository;
    private final ShortCodeGenerator generator;

    public ShortLinkService(ShortLinkRepository repository, ShortCodeGenerator generator) {
        this.repository = repository;
        this.generator = generator;
    }

    /**
     * Every call creates a new link, even for a URL shortened before: each link has its own expiry and stats.
     * Deliberately not @Transactional: each save commits on its own, so a code collision rolls back only that
     * attempt and the next attempt starts with a clean transaction.
     */
    public ShortLink shorten(String originalUrl, Instant expiresAt) {
        for (int attempt = 1; attempt <= MAX_CODE_ATTEMPTS; attempt++) {
            try {
                return repository.saveAndFlush(new ShortLink(generator.next(), originalUrl, expiresAt));
            } catch (DataIntegrityViolationException codeAlreadyTaken) {
                // Unique constraint on code hit (also covers two requests racing for the same code): retry.
            }
        }
        throw new IllegalStateException("Could not generate a unique short code after " + MAX_CODE_ATTEMPTS + " attempts");
    }

    /** Returns the URL to redirect to and counts the visit. */
    @Transactional
    public String visit(String code) {
        ShortLink link = find(code);
        if (link.isExpired(Instant.now())) {
            throw new ResourceGoneException("Short link " + code + " has expired");
        }
        repository.incrementVisitCount(link.getId());
        return link.getOriginalUrl();
    }

    @Transactional(readOnly = true)
    public ShortLink stats(String code) {
        return find(code);
    }

    private ShortLink find(String code) {
        return repository.findByCode(code).orElseThrow(() -> new ResourceNotFoundException("Short link", code));
    }
}
