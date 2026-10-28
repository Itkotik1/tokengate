package tokengate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import tokengate.domain.Cart;
import tokengate.domain.Customer;
import tokengate.repository.CustomerRepository;
import tokengate.service.StoreService;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customerRepository;
    private final StoreService storeService;

    // POST /api/customers - создать покупателя
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Customer createCustomer(@RequestBody Customer customer) {
        // При создании клиента сразу создаем ему пустую корзину
        customer.setCart(new Cart());
        return customerRepository.save(customer);
    }

    // GET /api/customers/{id}/cart - посмотреть корзину
    @GetMapping("/{id}/cart")
    public Cart getCart(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new tokengate.exception.EntityNotFoundException("Клиент не найден"));
        return customer.getCart();
    }

    // POST /api/customers/{id}/cart/items - добавить в корзину
    @PostMapping("/{id}/cart/items")
    public Cart addToCart(
            @PathVariable Long id,
            @RequestParam Long packageId,
            @RequestParam int quantity) {
        storeService.addToCart(id, packageId, quantity);
        // Возвращаем обновленную корзину
        return customerRepository.findById(id).get().getCart();
    }

    // GET /api/customers - получить всех клиентов
    @GetMapping
    public java.util.List<Customer> getAllCustomers() {
        return customerRepository.findAll();
    }

    @GetMapping("/{id}")
    public Customer getCustomer(@PathVariable Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new tokengate.exception.EntityNotFoundException("Клиент не найден"));
    }

    // PUT /api/customers/{id}/cart/items/{packageId} - изменить количество
    @PutMapping("/{id}/cart/items/{packageId}")
    public Cart updateCartItemQuantity(
            @PathVariable Long id,
            @PathVariable Long packageId,
            @RequestParam int quantity) {

        // Вызываем метод в сервисе (нам нужно его добавить в StoreService)
        return storeService.updateCartItemQuantity(id, packageId, quantity);
    }
}