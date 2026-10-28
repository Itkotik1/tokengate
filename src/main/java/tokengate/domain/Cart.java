package tokengate.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Entity
@Table(name = "carts")
@Getter @Setter @NoArgsConstructor
public class Cart {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Каскадное сохранение: сохраняем корзину — сохраняются и позиции
    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    public void addPackage(TokenPackage tokenPackage, int quantity) {
        Optional<CartItem> existingItem = items.stream()
                .filter(item -> item.getTokenPackage().getId().equals(tokenPackage.getId()))
                .findFirst();

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            int newQuantity = item.getQuantity() + quantity;

            // ПРОВЕРКА: Не превышает ли новое количество остаток на складе
            if (newQuantity > tokenPackage.getStock()) {
                throw new tokengate.exception.BusinessRuleException(
                        "Недостаточно товара на складе. В наличии: " + tokenPackage.getStock() + " шт."
                );
            }

            item.setQuantity(newQuantity);
        } else {
            // ПРОВЕРКА: Если добавляем впервые, не превышает ли кол-во остаток
            if (quantity > tokenPackage.getStock()) {
                throw new tokengate.exception.BusinessRuleException(
                        "Недостаточно товара на складе. В наличии: " + tokenPackage.getStock() + " шт."
                );
            }
            CartItem newItem = new CartItem();
            newItem.setCart(this);
            newItem.setTokenPackage(tokenPackage);
            newItem.setQuantity(quantity);
            items.add(newItem);
        }
    }

    public void clear() {
        items.clear();
    }

    public Double calculateTotal() {
        return items.stream().mapToDouble(CartItem::getItemTotal).sum();
    }
}