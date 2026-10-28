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

    // POST /api/orders?customerId=1 - оформить заказ
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public java.util.Map<String, Object> createOrder(@RequestParam Long customerId) {
        Order order = storeService.checkout(customerId);

        // Возвращаем только безопасные данные (без рекурсии)
        return java.util.Map.of(
                "orderId", order.getId(),
                "totalPrice", order.getTotalPrice(),
                "status", order.getStatus().name(),
                "message", "Заказ успешно оформлен"
        );
    }

    // GET /api/orders - получить все заказы
    @GetMapping
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    // PATCH /api/orders/{id}/status - изменить статус заказа
    @PatchMapping("/{id}/status")
    public Order changeStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new tokengate.exception.EntityNotFoundException("Заказ не найден"));

        if (status == OrderStatus.CANCELLED) {
            order.cancel(); // Вызовет бизнес-исключение, если отменить нельзя
        } else {
            order.setStatus(status);
        }
        return orderRepository.save(order);
    }
}