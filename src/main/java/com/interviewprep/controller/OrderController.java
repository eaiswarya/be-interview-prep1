package com.interviewprep.controller;

import com.interviewprep.dto.OrderRequest;
import com.interviewprep.dto.OrderResponse;
import com.interviewprep.dto.PlacedOrder;
import com.interviewprep.service.OrderService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    /**
     * The client generates an Idempotency-Key (e.g. a UUID) per order and resends the same key when retrying.
     * 201 = order created; 200 = a retry, returning the order created the first time.
     */
    @PostMapping
    public ResponseEntity<OrderResponse> place(@AuthenticationPrincipal Jwt token,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderRequest request) {
        PlacedOrder placed = service.place(userId(token), idempotencyKey, request);
        if (!placed.created()) {
            return ResponseEntity.ok(placed.order());
        }
        return ResponseEntity.created(URI.create("/api/orders/" + placed.order().id())).body(placed.order());
    }

    @GetMapping("/{id}")
    public OrderResponse get(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return service.get(userId(token), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderResponse cancel(@AuthenticationPrincipal Jwt token, @PathVariable Long id) {
        return service.cancel(userId(token), id);
    }

    private static Long userId(Jwt token) {
        return Long.valueOf(token.getSubject());
    }
}
