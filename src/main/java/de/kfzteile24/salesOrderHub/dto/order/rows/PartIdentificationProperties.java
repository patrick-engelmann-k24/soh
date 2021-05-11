package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

@Data
public class PartIdentificationProperties {
    private String carTypeNumber;
    private String prNumber;
    private String oeNumber;
    private String carSelectionType;
    private String hsn;
    private String tsn;
}
