package tokengate.domain;

import jakarta.persistence.*;
import lombok.*;
import tokengate.exception.BusinessRuleException;

@Entity
@Table(name = "token_packages")
@Getter @Setter @NoArgsConstructor
public class TokenPackage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // Связь: Один пакет относится к одной модели
    @JoinColumn(name = "model_id")
    private LlmModel model;

    private Integer tokenAmount;
    private Double price;
    private Integer stock;

    // Оставляем бизнес-методы
    public void decreaseStock(int quantity) {
        if (quantity <= 0) throw new BusinessRuleException("Количество должно быть > 0");
        if (this.stock < quantity) throw new BusinessRuleException("Недостаточно пакетов. Доступно: " + this.stock);
        this.stock -= quantity;
    }

    public void increaseStock(int quantity) {
        if (quantity <= 0) throw new BusinessRuleException("Количество должно быть > 0");
        this.stock += quantity;
    }
}