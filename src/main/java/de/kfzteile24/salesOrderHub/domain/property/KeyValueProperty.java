package de.kfzteile24.salesOrderHub.domain.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.time.LocalDateTime;

@Entity
@Table(name = "key_value_property", schema = "public", catalog = "soh")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class KeyValueProperty {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "id_generator_sequence")
    @SequenceGenerator(name = "id_generator_sequence", allocationSize = 1)
    private Long id;

    private String key;

    private String value;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @Transient
    private Object typedValue;
}
