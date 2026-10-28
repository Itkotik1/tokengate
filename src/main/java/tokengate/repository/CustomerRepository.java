package tokengate.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tokengate.domain.*;

import java.util.List;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {}

