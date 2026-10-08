package com.interviewprep.url;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Random Base62 codes: URL-safe without encoding, and 62^7 (about 3.5 trillion) combinations make
 * collisions rare. Random rather than sequential so codes cannot be enumerated to discover other links.
 */
@Component
public class ShortCodeGenerator {

    static final int LENGTH = 7;
    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final SecureRandom random = new SecureRandom();

    public String next() {
        StringBuilder code = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }
}
