package de.kfzteile24.salesOrderHub.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "invoice_number_counter", schema = "public", catalog = "soh")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class InvoiceNumberCounter {

    @Id
    @Column(name = "year", columnDefinition = "int", updatable = false)
    private Integer year;

    private Long counter;


}
