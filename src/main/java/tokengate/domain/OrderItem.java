package tokengate.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "order_items")
@Getter @Setter @NoArgsConstructor
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String modelName;
    private Integer tokenAmount;
    private Double price;
    private Integer quantity;

    public OrderItem(TokenPackage pkg, Integer quantity) {
        this.modelName = pkg.getModel().getName();
        this.tokenAmount = pkg.getTokenAmount();
        this.price = pkg.getPrice();
        this.quantity = quantity;
    }

    public Double getItemTotal() {
        return this.price * this.quantity;
    }
}