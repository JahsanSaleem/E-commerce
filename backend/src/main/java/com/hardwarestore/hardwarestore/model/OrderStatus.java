package com.hardwarestore.hardwarestore.model;

import java.util.Set;

public enum OrderStatus {
    PENDING, CONFIRMED, PROCESSING, SHIPPED, READY_FOR_COLLECTION, DELIVERED, CANCELLED;

    public Set<OrderStatus> nextStatuses() {
        return switch (this) {
            case PENDING -> Set.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> Set.of(PROCESSING, CANCELLED);
            case PROCESSING -> Set.of(SHIPPED, READY_FOR_COLLECTION, CANCELLED);
            case SHIPPED, READY_FOR_COLLECTION -> Set.of(DELIVERED);
            case DELIVERED, CANCELLED -> Set.of();
        };
    }
}
