package de.kfzteile24.salesOrderHub.dto.property;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PersistentProperty {

    private String key;
    private Object value;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
