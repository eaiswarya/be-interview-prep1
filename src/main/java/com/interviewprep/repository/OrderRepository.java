package com.interviewprep.repository;

import com.interviewprep.model.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findByUserIdAndIdempotencyKey(Long userId, String idempotencyKey);

    /** Scoped to the owner: another user's order id behaves exactly like a missing one. */
    @EntityGraph(attributePaths = "items")
    Optional<Order> findByIdAndUserId(Long id, Long userId);

    /**
     * Moves PLACED to CANCELLED in one statement. Two concurrent cancels cannot both see PLACED,
     * so stock is returned exactly once.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Order o set o.status = com.interviewprep.model.OrderStatus.CANCELLED "
            + "where o.id = :id and o.status = com.interviewprep.model.OrderStatus.PLACED")
    int cancelIfPlaced(@Param("id") Long id);
}
