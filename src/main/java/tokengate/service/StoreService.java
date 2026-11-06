package tokengate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tokengate.domain.*;
import tokengate.exception.BusinessRuleException;
import tokengate.exception.EntityNotFoundException;
import tokengate.repository.*;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StoreService {

    private final LlmModelRepository modelRepository;
    private final TokenPackageRepository packageRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    @Transactional
    public void addToCart(Long customerId, Long packageId, int quantity) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));
        TokenPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new EntityNotFoundException("Пакет не найден"));
        customer.getCart().addPackage(pkg, quantity);
        customerRepository.save(customer);
    }

    @Transactional
    public Cart updateCartItemQuantity(Long customerId, Long packageId, int newQuantity) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));
        Cart cart = customer.getCart();
        CartItem itemToUpdate = cart.getItems().stream()
                .filter(item -> item.getTokenPackage().getId().equals(packageId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException("Пакет не найден в корзине"));

        if (newQuantity <= 0) {
            cart.getItems().remove(itemToUpdate);
        } else {
            if (newQuantity > itemToUpdate.getTokenPackage().getStock()) {
                throw new BusinessRuleException("Недостаточно товара на складе. В наличии: " + itemToUpdate.getTokenPackage().getStock() + " шт.");
            }
            itemToUpdate.setQuantity(newQuantity);
        }
        customerRepository.save(customer);
        return cart;
    }

    @Transactional
    public Order checkout(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));

        Cart cart = customer.getCart();
        if (cart.getItems().isEmpty()) {
            throw new BusinessRuleException("Корзина пуста!");
        }

        Double totalPrice = cart.calculateTotal();
        customer.deductBalance(totalPrice);

        Order order = new Order();
        order.setCustomer(customer);
        order = orderRepository.save(order); // Сначала сохраняем заказ

        for (CartItem item : cart.getItems()) {
            item.getTokenPackage().decreaseStock(item.getQuantity());
            OrderItem orderItem = new OrderItem(item.getTokenPackage(), item.getQuantity());
            order.addItem(orderItem);
        }

        cart.clear();
        customer.addOrder(order);
        customerRepository.save(customer);

        return orderRepository.save(order);
    }

    @Transactional
    public Order changeOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new EntityNotFoundException("Заказ не найден"));

        switch (newStatus) {
            case CONFIRMED -> order.confirm();
            case COMPLETED -> order.complete();
            case CANCELLED -> {
                order.cancel();
                // Возврат остатков на склад
                for (OrderItem item : order.getItems()) {
                    packageRepository.findById(item.getPackageId()).ifPresent(pkg -> {
                        pkg.increaseStock(item.getQuantity());
                        packageRepository.save(pkg);
                    });
                }
                // Возврат денег
                order.getCustomer().refundBalance(order.getTotalPrice());
                customerRepository.save(order.getCustomer());
            }
            default -> throw new BusinessRuleException("Нельзя установить статус " + newStatus);
        }
        return orderRepository.save(order);
    }

    public List<TokenPackage> findPackagesByProvider(ModelProvider provider) {
        return packageRepository.findAll().stream()
                .filter(pkg -> pkg.getModel().getProvider() == provider)
                .collect(Collectors.toList());
    }

    public Double getTotalCustomerSpent(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));
        return customer.getOrders().stream()
                .mapToDouble(Order::getTotalPrice)
                .sum();
    }

    public Optional<TokenPackage> findCheapestPackage() {
        return packageRepository.findAll().stream()
                .min(Comparator.comparing(TokenPackage::getPrice));
    }

    public Map<OrderStatus, List<Order>> groupOrdersByStatus() {
        return orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(Order::getStatus));
    }
}