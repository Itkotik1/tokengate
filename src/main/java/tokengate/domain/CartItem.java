package tokengate.domain;

import jakarta.persistence.*;
import lombok.*;
import tokengate.exception.BusinessRuleException;
import com.fasterxml.jackson.annotation.JsonIgnore; // Импорт

@Entity
@Table(name = "cart_items")
@Getter @Setter @NoArgsConstructor
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "cart_id")
    @JsonIgnore
    private Cart cart;

    @ManyToOne
    @JoinColumn(name = "package_id")
    private TokenPackage tokenPackage;

    private Integer quantity;

    public Double getItemTotal() {
        return this.tokenPackage.getPrice() * this.quantity;
    }

    public void setQuantity(Integer quantity) {
        if (quantity <= 0) throw new BusinessRuleException("Количество в корзине должно быть > 0");
        this.quantity = quantity;
    }
}