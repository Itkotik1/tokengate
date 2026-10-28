package tokengate.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tokengate.domain.*;
import tokengate.exception.EntityNotFoundException;
import tokengate.repository.*;
import tokengate.service.StoreService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final StoreService storeService;
    private final CustomerRepository customerRepository;

    // 1. Агрегация: Общая сумма, потраченная клиентом на все заказы
    // GET /api/analytics/customer/1/spent
    @GetMapping("/customer/{id}/spent")
    public Map<String, Object> getCustomerSpent(@PathVariable Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Клиент не найден"));

        Double totalSpent = storeService.getTotalCustomerSpent(id);

        // Возвращаем красивый JSON
        return Map.of(
                "customerName", customer.getName(),
                "totalSpent", totalSpent
        );
    }

    // 2. Фильтрация и Сортировка: Найти самый дешевый пакет в каталоге
    // GET /api/analytics/packages/cheapest
    @GetMapping("/packages/cheapest")
    public TokenPackage getCheapestPackage() {
        return storeService.findCheapestPackage()
                .orElseThrow(() -> new EntityNotFoundException("В каталоге нет пакетов"));
    }

    // 3. Группировка: Сгруппировать все заказы по статусу
    // GET /api/analytics/orders/group-by-status
    @GetMapping("/orders/group-by-status")
    public Map<OrderStatus, List<Order>> groupOrdersByStatus() {
        return storeService.groupOrdersByStatus();
    }

    // 4. Фильтрация: Найти все пакеты конкретного провайдера (например, OPENAI)
    // GET /api/analytics/packages/provider/OPENAI
    @GetMapping("/packages/provider/{provider}")
    public List<TokenPackage> getPackagesByProvider(@PathVariable ModelProvider provider) {
        return storeService.findPackagesByProvider(provider);
    }
}