package de.kfzteile24.salesOrderHub.domain.dropshipment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.UUID;

@Entity
@Table(name = "dropshipment_invoice_row", schema = "public", catalog = "soh")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Valid
public class DropshipmentInvoiceRow {

    @Id
    @Column(name = "id", columnDefinition = "uuid", updatable = false)
    @GeneratedValue
    private UUID id;

    @NotNull
    private String sku;

    @NotNull
    private String orderNumber;

    private String invoiceNumber;

    @NotNull
    private Integer quantity;

    public void addQuantity(Integer quantity) {
        this.quantity = this.quantity + quantity;
    }
}
