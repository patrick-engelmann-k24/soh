package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

@Data
public class PartIdentificationProperties {
    String carTypeNumber;
    String prNumber;
    String oeNumber;
    String carSelectionType;
    String hsn;
    String tsn;
}
