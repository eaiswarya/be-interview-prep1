package com.interviewprep.url;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ShortLinkServiceTest {

    ShortLinkRepository repository = mock(ShortLinkRepository.class);
    ShortCodeGenerator generator = mock(ShortCodeGenerator.class);
    ShortLinkService service = new ShortLinkService(repository, generator);

    @Test
    void retriesWithANewCodeWhenTheCodeIsAlreadyTaken() {
        when(generator.next()).thenReturn("taken01", "fresh01");
        when(repository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException("duplicate code"))
                .thenAnswer(call -> call.getArgument(0));

        ShortLink link = service.shorten("https://example.com", null);

        assertThat(link.getCode()).isEqualTo("fresh01");
        verify(repository, times(2)).saveAndFlush(any());
    }

    @Test
    void givesUpAfterTheMaximumNumberOfAttempts() {
        when(generator.next()).thenReturn("taken01");
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate code"));

        assertThatThrownBy(() -> service.shorten("https://example.com", null))
                .isInstanceOf(IllegalStateException.class);
        verify(repository, times(ShortLinkService.MAX_CODE_ATTEMPTS)).saveAndFlush(any());
    }

    @Test
    void generatedCodesAreUrlSafeAndAtMostEightCharacters() {
        ShortCodeGenerator real = new ShortCodeGenerator();
        for (int i = 0; i < 1000; i++) {
            assertThat(real.next()).matches("[0-9A-Za-z]{1,8}");
        }
    }
}
