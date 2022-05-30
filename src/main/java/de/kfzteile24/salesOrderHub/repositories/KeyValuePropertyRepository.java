package de.kfzteile24.salesOrderHub.repositories;

import de.kfzteile24.salesOrderHub.domain.property.KeyValueProperty;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KeyValuePropertyRepository extends JpaRepository<KeyValueProperty, Long> {

    Optional<KeyValueProperty> findByKey(String key);
}
