package tokengate.domain;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "llm_models")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LlmModel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING) // Сохраняем в БД как строку (OPENAI, ANTHROPIC)
    private ModelProvider provider;

    private String description;
    private Double pricePer1000Tokens;
    private Integer contextWindow;

    // equals и hashCode оставляем (для защиты)
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LlmModel llmModel = (LlmModel) o;
        return java.util.Objects.equals(id, llmModel.id) && java.util.Objects.equals(name, llmModel.name);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name);
    }
}