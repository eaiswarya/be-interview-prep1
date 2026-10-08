package com.interviewprep.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record OrderRequest(
        @NotEmpty(message = "items must contain at least one item")
        @Size(max = 50, message = "items must contain at most 50 items")
        List<@Valid @NotNull(message = "item must not be null") OrderItemRequest> items) {
}
