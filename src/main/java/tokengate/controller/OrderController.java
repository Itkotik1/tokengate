package tokengate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tokengate.domain.Order;
import tokengate.domain.OrderStatus;
import tokengate.repository.OrderRepository;
import tokengate.service.StoreService;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final StoreService storeService;
    private final OrderRepository orderRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public java.util.Map<String, Object> createOrder(@RequestParam Long customerId) {
        Order order = storeService.checkout(customerId);
        return java.util.Map.of(
                "orderId", order.getId(),
                "totalPrice", order.getTotalPrice(),
                "status", order.getStatus().name(),
                "message", "Заказ успешно оформлен"
        );
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    @GetMapping("/{id}")
    public Order getOrder(@PathVariable Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new tokengate.exception.EntityNotFoundException("Заказ не найден"));
    }

    @PatchMapping("/{id}/status")
    public Order changeStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        return storeService.changeOrderStatus(id, status);
    }
}