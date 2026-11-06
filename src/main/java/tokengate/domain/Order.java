package tokengate.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import tokengate.exception.BusinessRuleException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter @Setter @NoArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime orderDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    private OrderStatus status = OrderStatus.NEW;

    private Double totalPrice = 0.0;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    @JsonIgnore
    private Customer customer;

    // Каскадное сохранение и удаление позиций заказа
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
        recalculateTotal();
    }

    private void recalculateTotal() {
        this.totalPrice = items.stream().mapToDouble(OrderItem::getItemTotal).sum();
    }

    public void confirm() {
        if (this.status != OrderStatus.NEW) {
            throw new BusinessRuleException("Подтвердить можно только новый заказ (NEW). Текущий статус: " + this.status);
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void complete() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new BusinessRuleException("Завершить можно только подтвержденный заказ (CONFIRMED). Текущий статус: " + this.status);
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        if (this.status == OrderStatus.COMPLETED || this.status == OrderStatus.CANCELLED) {
            throw new BusinessRuleException("Нельзя отменить заказ в статусе " + this.status);
        }
        this.status = OrderStatus.CANCELLED;
    }
}