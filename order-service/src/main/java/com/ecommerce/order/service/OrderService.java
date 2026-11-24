package com.ecommerce.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.ecommerce.common.exception.BadRequestException;
import com.ecommerce.common.exception.ConflictException;
import com.ecommerce.common.exception.ForbiddenException;
import com.ecommerce.common.exception.NotFoundException;
import com.ecommerce.order.audit.AuditPublisher;
import com.ecommerce.order.client.CartClient;
import com.ecommerce.order.client.CartClient.CartDto;
import com.ecommerce.order.client.CartClient.CartItemDto;
import com.ecommerce.order.client.PaymentClient;
import com.ecommerce.order.client.PaymentClient.PaymentRequest;
import com.ecommerce.order.client.PaymentClient.PaymentResult;
import com.ecommerce.order.client.ProductClient;
import com.ecommerce.order.client.ShippingClient;
import com.ecommerce.order.client.ShippingClient.ShipmentRequest;
import com.ecommerce.order.client.ShippingClient.ShipmentResult;
import com.ecommerce.order.client.UserClient;
import com.ecommerce.order.client.UserClient.UserDto;
import com.ecommerce.order.dto.Dtos.CheckoutRequest;
import com.ecommerce.order.dto.Dtos.OrderResponse;
import com.ecommerce.order.entity.CustomerOrder;
import com.ecommerce.order.entity.OrderStatus;
import com.ecommerce.order.mapper.OrderMapper;
import com.ecommerce.order.repository.OrderRepository;

/**
 * The heart of the system. checkout() is a list of small steps, each one calls ANOTHER microservice:
 *
 *   1. user-service      who is buying?
 *   2. cart-service      what is in the cart?
 *   3. (own database)    save the order as PENDING
 *   4. product-service   take the items out of stock
 *   5. payment-service   charge the customer      (fails -> put the stock back)
 *   6. shipping-service  create the delivery
 *   7. cart-service      empty the cart
 *   and Kafka            every important step is also sent as an "audit event"
 */
@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository repository;
    private final OrderMapper mapper;
    private final UserClient userClient;
    private final CartClient cartClient;
    private final ProductClient productClient;
    private final PaymentClient paymentClient;
    private final ShippingClient shippingClient;
    private final AuditPublisher audit;

    public OrderService(OrderRepository repository, OrderMapper mapper, UserClient userClient,
                        CartClient cartClient, ProductClient productClient, PaymentClient paymentClient,
                        ShippingClient shippingClient, AuditPublisher audit) {
        this.repository = repository;
        this.mapper = mapper;
        this.userClient = userClient;
        this.cartClient = cartClient;
        this.productClient = productClient;
        this.paymentClient = paymentClient;
        this.shippingClient = shippingClient;
        this.audit = audit;
    }

    public OrderResponse checkout(Long userId, CheckoutRequest request) {
        // Step 1 + 2: who is buying and what?
        UserDto user = userClient.getUser(userId);
        CartDto cart = cartClient.getCart(userId);
        if (cart == null || cart.items() == null || cart.items().isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        // Step 3: save the order
        CustomerOrder order = createOrder(user, cart, request);

        // Step 4: stock
        List<CartItemDto> reserved = reserveStock(order, cart);

        // Step 5: payment
        PaymentResult payment = takePayment(order, reserved, request);
        if (!payment.isSuccess()) {
            giveStockBack(reserved);
            closeOrder(order, OrderStatus.PAYMENT_FAILED, payment.message(), "PAYMENT_FAILED");
            return mapper.toResponse(order);                      // controller turns this into HTTP 402
        }
        order.setStatus(OrderStatus.PAID);
        order.setPaymentTransactionId(payment.transactionId());
        order = save(order);
        audit.publish("PAYMENT_SUCCESS", order.getId(), userId,
                Map.of("transactionId", payment.transactionId(), "amount", order.getTotalAmount()));

        // Step 6: shipment - the customer already paid, so a shipping problem must NOT cancel the order
        order = createShipment(order, request);

        // Step 7: empty the cart (also not critical)
        try {
            cartClient.clearCart(userId);
        } catch (RuntimeException e) {
            log.warn("Could not empty the cart of user {}: {}", userId, e.getMessage());
        }
        return mapper.toResponse(order);
    }

    // ------------------------------------------------------------------ small steps

    private CustomerOrder createOrder(UserDto user, CartDto cart, CheckoutRequest request) {
        CustomerOrder order = new CustomerOrder();
        order.setUserId(user.id());
        order.setCustomerName(user.username());
        order.setStatus(OrderStatus.PENDING);
        order.setShippingAddress(request.shippingAddress());
        order.setPaymentMethod(request.paymentMethod());
        order.setCreatedAt(Instant.now());
        order.setUpdatedAt(order.getCreatedAt());

        BigDecimal total = BigDecimal.ZERO;
        for (CartItemDto cartLine : cart.items()) {
            var orderLine = mapper.toOrderItem(cartLine);          // MapStruct: cart line -> order line
            orderLine.setOrder(order);
            order.getItems().add(orderLine);
            total = total.add(cartLine.unitPrice().multiply(BigDecimal.valueOf(cartLine.quantity())));
        }
        order.setTotalAmount(total);

        order = repository.save(order);
        audit.publish("ORDER_CREATED", order.getId(), user.id(),
                Map.of("total", order.getTotalAmount(), "items", order.getItems().size()));
        return order;
    }

    /** Takes every cart line out of stock. If one fails, the ones already taken are given back. */
    private List<CartItemDto> reserveStock(CustomerOrder order, CartDto cart) {
        List<CartItemDto> reserved = new ArrayList<>();
        try {
            for (CartItemDto line : cart.items()) {
                productClient.reduceStock(line.productId(), line.quantity());
                reserved.add(line);
            }
            return reserved;
        } catch (RuntimeException e) {
            giveStockBack(reserved);
            closeOrder(order, OrderStatus.CANCELLED, e.getMessage(), "ORDER_CANCELLED");
            throw e;
        }
    }

    private PaymentResult takePayment(CustomerOrder order, List<CartItemDto> reserved, CheckoutRequest request) {
        try {
            return paymentClient.pay(new PaymentRequest(order.getId(), order.getUserId(),
                    order.getTotalAmount(), request.paymentMethod()));
        } catch (RuntimeException e) {                              // payment-service is down
            giveStockBack(reserved);
            closeOrder(order, OrderStatus.CANCELLED, "Payment service error: " + e.getMessage(), "ORDER_CANCELLED");
            throw e;
        }
    }

    private CustomerOrder createShipment(CustomerOrder order, CheckoutRequest request) {
        try {
            ShipmentResult shipment = shippingClient.createShipment(
                    new ShipmentRequest(order.getId(), order.getUserId(), request.shippingAddress()));
            order.setTrackingNumber(shipment.trackingNumber());
            order = save(order);
            audit.publish("SHIPMENT_CREATED", order.getId(), order.getUserId(),
                    Map.of("trackingNumber", shipment.trackingNumber()));
        } catch (RuntimeException e) {
            log.error("Shipment could not be created for order {} (order stays PAID): {}", order.getId(), e.getMessage());
            audit.publish("SHIPMENT_FAILED", order.getId(), order.getUserId(), Map.of("reason", String.valueOf(e.getMessage())));
        }
        return order;
    }

    // ------------------------------------------------------------------ queries and status changes

    public List<OrderResponse> listForUser(Long userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(mapper::toResponse).toList();
    }

    public List<OrderResponse> listAll() {
        return repository.findAll().stream().map(mapper::toResponse).toList();
    }

    public OrderResponse get(Long id, Long callerId, String role) {
        CustomerOrder order = find(id);
        if (!order.getUserId().equals(callerId) && !"ADMIN".equals(role)) {
            throw new ForbiddenException("This order belongs to another user");
        }
        return mapper.toResponse(order);
    }

    /** Called by shipping-service when the parcel moves: PAID -> SHIPPED -> DELIVERED. */
    public OrderResponse updateStatus(Long id, OrderStatus next) {
        CustomerOrder order = find(id);
        boolean allowed = (order.getStatus() == OrderStatus.PAID && next == OrderStatus.SHIPPED)
                || (order.getStatus() == OrderStatus.SHIPPED && next == OrderStatus.DELIVERED);
        if (!allowed) {
            throw new ConflictException("Cannot move order " + id + " from " + order.getStatus() + " to " + next);
        }
        OrderStatus previous = order.getStatus();
        order.setStatus(next);
        order = save(order);
        audit.publish("ORDER_STATUS_CHANGED", id, order.getUserId(), Map.of("from", previous.name(), "to", next.name()));
        return mapper.toResponse(order);
    }

    // ------------------------------------------------------------------ helpers

    private CustomerOrder find(Long id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Order not found: " + id));
    }

    private CustomerOrder save(CustomerOrder order) {
        order.setUpdatedAt(Instant.now());
        return repository.save(order);
    }

    private void closeOrder(CustomerOrder order, OrderStatus status, String reason, String eventType) {
        order.setStatus(status);
        order.setFailureReason(reason);
        save(order);
        audit.publish(eventType, order.getId(), order.getUserId(), Map.of("reason", String.valueOf(reason)));
    }

    private void giveStockBack(List<CartItemDto> reserved) {
        for (CartItemDto line : reserved) {
            try {
                productClient.restoreStock(line.productId(), line.quantity());
            } catch (RuntimeException e) {
                log.error("COULD NOT GIVE STOCK BACK: product {} quantity {} ({})", line.productId(), line.quantity(), e.getMessage());
            }
        }
    }
}
