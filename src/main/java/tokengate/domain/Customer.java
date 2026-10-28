package tokengate.domain;

import com.fasterxml.jackson.annotation.JsonIgnore; // Импорт

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "customers")
@Getter @Setter @NoArgsConstructor
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String email;

    private Double balance = 10000.0;

    @OneToOne(cascade = CascadeType.ALL)
    @JsonIgnore
    @JoinColumn(name = "cart_id", referencedColumnName = "id")
    private Cart cart;

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
    private List<Order> orders = new ArrayList<>();

    public void addOrder(Order order) {
        this.orders.add(order);
        order.setCustomer(this);
    }

    public void deductBalance(Double amount) {
        if (this.balance < amount) {
            throw new tokengate.exception.BusinessRuleException("Недостаточно средств на балансе! Текущий баланс: " + this.balance);
        }
        this.balance -= amount;
    }

}