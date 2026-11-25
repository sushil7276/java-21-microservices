package com.ecommerce.order.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.ecommerce.order.entity.CustomerOrder;
import com.ecommerce.order.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<CustomerOrder, Long> {
    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    /** Orders in the given statuses created in [from, to). Pageable = which page of how many rows. */
    Page<CustomerOrder> findByStatusInAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            Collection<OrderStatus> statuses, Instant from, Instant to, Pageable pageable);
}
