package tokengate.controller;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tokengate.domain.Cart;
import tokengate.domain.Customer;
import tokengate.domain.Order;
import tokengate.exception.EntityNotFoundException;
import tokengate.repository.CustomerRepository;
import tokengate.service.StoreService;
import tokengate.repository.OrderRepository;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final StoreService storeService;
    private final OrderRepository orderRepository;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer createCustomer(@RequestBody Customer customer) {
        customer.setCart(new Cart());
        return customerRepository.save(customer);
    }

    @GetMapping("/{id}/cart")
    public Cart getCart(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));
        return customer.getCart();
    }

    @PostMapping("/{id}/cart/items")
    public Cart addToCart(
            @PathVariable Long id,
            @RequestParam Long packageId,
            @RequestParam int quantity) {
        storeService.addToCart(id, packageId, quantity);
        return customerRepository.findById(id).get().getCart();
    }

    @GetMapping
    public List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Customer getCustomer(@PathVariable Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));
    }

    @PutMapping("/{id}/cart/items/{packageId}")
    public Cart updateCartItemQuantity(
            @PathVariable Long id,
            @PathVariable Long packageId,
            @RequestParam int quantity) {
        return storeService.updateCartItemQuantity(id, packageId, quantity);
    }

    @GetMapping("/{id}/orders")
    public List<Order> getCustomerOrders(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));
        return customer.getOrders();
    }

    @DeleteMapping("/{id}/orders")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional // ВАЖНО: Без этого Postgres выдаст ошибку целостности
    public void clearOrderHistory(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));

        // 1. Копируем список заказов в отдельную переменную
        java.util.List<Order> ordersToDelete = new java.util.ArrayList<>(customer.getOrders());

        // 2. Очищаем связь у клиента
        customer.getOrders().clear();
        customerRepository.save(customer);

        // 3. Удаляем сами заказы.
        // Благодаря cascade = CascadeType.ALL в классе Order,
        // Hibernate автоматически удалит все OrderItem, привязанные к этим заказам!
        orderRepository.deleteAll(ordersToDelete);
    }
}
