package com.ecommerce.shipping.service;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecommerce.shipping.exception.ConflictException;
import com.ecommerce.shipping.exception.NotFoundException;
import com.ecommerce.shipping.client.OrderClient;
import com.ecommerce.shipping.entity.Shipment;
import com.ecommerce.shipping.entity.ShipmentStatus;
import com.ecommerce.shipping.dto.Dtos.CreateShipmentRequest;
import com.ecommerce.shipping.dto.ShipmentResponse;
import com.ecommerce.shipping.mapper.ShipmentMapper;
import com.ecommerce.shipping.repository.ShipmentRepository;

@Service
public class ShipmentService {

    private final ShipmentRepository repository;
    private final OrderClient orderClient;
    private final ShipmentMapper mapper;

    public ShipmentService(ShipmentRepository repository, OrderClient orderClient, ShipmentMapper mapper) {
        this.repository = repository;
        this.orderClient = orderClient;
        this.mapper = mapper;
    }

    public ShipmentResponse create(CreateShipmentRequest req) {
        Shipment shipment = repository.findByOrderId(req.orderId()).orElseGet(() -> repository.save(Shipment.builder()
                .orderId(req.orderId()).userId(req.userId()).address(req.address())
                .trackingNumber("TRK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase())
                .status(ShipmentStatus.CREATED)
                .createdAt(Instant.now()).updatedAt(Instant.now())
                .build()));
        return mapper.toResponse(shipment);
    }

    public ShipmentResponse byOrder(Long orderId) {
        return mapper.toResponse(repository.findByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException("No shipment for order " + orderId)));
    }

    public ShipmentResponse byTracking(String trackingNumber) {
        return mapper.toResponse(repository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new NotFoundException("Unknown tracking number " + trackingNumber)));
    }

    /** Moves the shipment forward and keeps order-service in sync (rolled back if the order call fails). */
    @Transactional
    public ShipmentResponse updateStatus(Long id, ShipmentStatus next) {
        Shipment shipment = repository.findById(id).orElseThrow(() -> new NotFoundException("Shipment not found: " + id));
        if (next.ordinal() <= shipment.getStatus().ordinal()) {
            throw new ConflictException("Shipment is already " + shipment.getStatus() + ", cannot move to " + next);
        }
        shipment.setStatus(next);
        shipment.setUpdatedAt(Instant.now());
        if (next == ShipmentStatus.SHIPPED || next == ShipmentStatus.DELIVERED) {
            // DELIVERED requires the order to be SHIPPED first - the order service enforces that
            orderClient.updateOrderStatus(shipment.getOrderId(), next.name());
        }
        return mapper.toResponse(shipment);
    }
}
