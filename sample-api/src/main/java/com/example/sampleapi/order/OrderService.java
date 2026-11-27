package com.example.sampleapi.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.sampleapi.product.Product;
import com.example.sampleapi.product.ProductNotFoundException;
import com.example.sampleapi.product.ProductRepository;
import com.example.sampleapi.user.UserNotFoundException;
import com.example.sampleapi.user.UserRepository;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, UserRepository userRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    @Transactional
    public OrderResponse create(OrderRequest request) {
        if (!userRepository.existsById(request.userId())) {
            throw new UserNotFoundException(request.userId());
        }
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ProductNotFoundException(request.productId()));
        validateStock(product, request.quantity());

        BigDecimal totalPrice = product.getPrice()
                .multiply(BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        Order order = new Order(
                request.userId(),
                product.getId(),
                request.quantity(),
                product.getPrice(),
                totalPrice,
                OrderStatus.PENDING);
        return OrderResponse.from(orderRepository.save(order));
    }

    public OrderResponse get(Long id) {
        return OrderResponse.from(findOrder(id));
    }

    @Transactional
    public OrderResponse update(Long id, OrderUpdateRequest request) {
        Order order = findOrder(id);

        if (order.getStatus() == OrderStatus.SHIPPED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderConflictException(
                    "Order " + id + " cannot be modified when status is " + order.getStatus());
        }
        if (order.getStatus() == OrderStatus.PAID && !request.quantity().equals(order.getQuantity())) {
            throw new OrderConflictException("Quantity can only be changed while the order is PENDING");
        }
        if (!isAllowedTransition(order.getStatus(), request.status())) {
            throw new OrderConflictException(
                    "Cannot change order status from " + order.getStatus() + " to " + request.status());
        }

        Product product = productRepository.findById(order.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(order.getProductId()));
        validateStock(product, request.quantity());

        BigDecimal totalPrice = order.getUnitPrice()
                .multiply(BigDecimal.valueOf(request.quantity()))
                .setScale(2, RoundingMode.HALF_UP);
        order.update(request.quantity(), totalPrice, request.status());
        return OrderResponse.from(order);
    }

    @Transactional
    public OrderResponse cancel(Long id) {
        Order order = findOrder(id);
        if (order.getStatus() == OrderStatus.SHIPPED) {
            throw new OrderConflictException("A shipped order cannot be cancelled");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new OrderConflictException("Order " + id + " is already cancelled");
        }
        order.cancel();
        return OrderResponse.from(order);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new OrderNotFoundException(id));
    }

    private static void validateStock(Product product, int requestedQuantity) {
        if (requestedQuantity > product.getStock()) {
            throw new InsufficientStockException(product.getId(), requestedQuantity, product.getStock());
        }
    }

    private static boolean isAllowedTransition(OrderStatus from, OrderStatus to) {
        return switch (from) {
            case PENDING -> to == OrderStatus.PENDING || to == OrderStatus.PAID;
            case PAID -> to == OrderStatus.PAID || to == OrderStatus.SHIPPED;
            case SHIPPED, CANCELLED -> false;
        };
    }
}
