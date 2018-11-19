package fishing.lee.backend;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;

import java.util.UUID;

@EnableScan
public interface BasketRepository extends CrudRepository<BasketModel, UUID> {
}
