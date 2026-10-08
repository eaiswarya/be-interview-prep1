package com.interviewprep.dto;

import com.interviewprep.model.Order;
import com.interviewprep.model.OrderStatus;
import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id, OrderStatus status, List<Item> items, Instant createdAt) {

    public record Item(Long productId, int quantity) {
    }

    public static OrderResponse from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(item -> new Item(item.getProductId(), item.getQuantity()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus(), items, order.getCreatedAt());
    }
}
