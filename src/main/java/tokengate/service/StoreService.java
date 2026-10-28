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

    // Внедряем репозитории (Dependency Injection)
    private final LlmModelRepository modelRepository;
    private final TokenPackageRepository packageRepository;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;

    // --- Бизнес-логика ---

    @Transactional
    public void addToCart(Long customerId, Long packageId, int quantity) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));

        TokenPackage pkg = packageRepository.findById(packageId)
                .orElseThrow(() -> new EntityNotFoundException("Пакет не найден"));

        customer.getCart().addPackage(pkg, quantity);
        customerRepository.save(customer); // Сохраняем изменения в БД
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

        for (CartItem item : cart.getItems()) {
            // Проверяем остатки на складе (метод decreaseStock выбросит ошибку, если не хватит)
            item.getTokenPackage().decreaseStock(item.getQuantity());

            // Создаем позицию заказа (фиксируем цену)
            OrderItem orderItem = new OrderItem(item.getTokenPackage(), item.getQuantity());
            order.addItem(orderItem);
        }

        // Очищаем корзину и сохраняем заказ
        cart.clear();
        customer.addOrder(order);

        customerRepository.save(customer);

        // Каскадное сохранение (CascadeType.ALL) сохранит и Order, и OrderItem, и обновит TokenPackage
        return orderRepository.save(order);
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
            // Если количество 0 - удаляем
            cart.getItems().remove(itemToUpdate);
        } else {
            // ПРОВЕРКА: Не превышает ли новое количество остаток на складе
            if (newQuantity > itemToUpdate.getTokenPackage().getStock()) {
                throw new BusinessRuleException(
                        "Недостаточно товара на складе. В наличии: " + itemToUpdate.getTokenPackage().getStock() + " шт."
                );
            }
            // Иначе меняем количество
            itemToUpdate.setQuantity(newQuantity);
        }

        customerRepository.save(customer);
        return cart;
    }
    // --- Stream API (для защиты) ---

    // 1. Фильтрация: найти все пакеты для конкретного провайдера
    public List<TokenPackage> findPackagesByProvider(ModelProvider provider) {
        // Загружаем все пакеты из БД и фильтруем через Stream
        return packageRepository.findAll().stream()
                .filter(pkg -> pkg.getModel().getProvider() == provider)
                .collect(Collectors.toList());
    }

    // 2. Агрегация: посчитать общую стоимость всех заказов клиента
    public Double getTotalCustomerSpent(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));

        return customer.getOrders().stream()
                .mapToDouble(Order::getTotalPrice)
                .sum();
    }

    // 3. Сортировка: найти самый дешевый пакет
    public Optional<TokenPackage> findCheapestPackage() {
        return packageRepository.findAll().stream()
                .min(Comparator.comparing(TokenPackage::getPrice));
    }

    // 4. Группировка: сгруппировать все заказы в системе по статусу
    public Map<OrderStatus, List<Order>> groupOrdersByStatus() {
        return orderRepository.findAll().stream()
                .collect(Collectors.groupingBy(Order::getStatus));
    }
}