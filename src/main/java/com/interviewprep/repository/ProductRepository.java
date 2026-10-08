package com.interviewprep.repository;

import com.interviewprep.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Reserves stock only if enough is left, as one atomic statement: the database locks the row, checks
     * the condition and subtracts together, so concurrent orders can never drive stock below zero.
     * Returns 0 when the product is missing or has too little stock.
     */
    @Modifying
    @Query("update Product p set p.stock = p.stock - :quantity where p.id = :id and p.stock >= :quantity")
    int reserveStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("update Product p set p.stock = p.stock + :quantity where p.id = :id")
    int releaseStock(@Param("id") Long id, @Param("quantity") int quantity);
}
