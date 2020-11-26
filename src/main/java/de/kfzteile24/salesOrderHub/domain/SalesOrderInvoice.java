package de.kfzteile24.salesOrderHub.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sales_order_invoice", schema = "public", catalog = "soh")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class SalesOrderInvoice extends AbstractBaseEntity {
    // important!
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Basic
    @Column(name = "invoice_number")
    private String invoiceNumber;

    @Basic
    @Column(name = "url")
    private String url;

    @Basic
    @Column(name = "customer_access_token")
    private String customerAccessToken;

    @Basic
    @Column(name = "created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Basic
    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;
}
