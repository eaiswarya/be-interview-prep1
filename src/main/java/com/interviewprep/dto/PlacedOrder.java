package com.interviewprep.dto;

/** The order, and whether this request created it (false = a retry that returned the original order). */
public record PlacedOrder(OrderResponse order, boolean created) {
}
