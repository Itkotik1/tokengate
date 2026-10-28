package tokengate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokengate.domain.TokenPackage;

import java.util.List;

@Repository
public interface TokenPackageRepository extends JpaRepository<TokenPackage, Long> {
    // Spring Data JPA поймет этот метод по названию и напишет SQL: SELECT * FROM token_packages WHERE model_id = ?
    List<TokenPackage> findByModelId(Long modelId);
}