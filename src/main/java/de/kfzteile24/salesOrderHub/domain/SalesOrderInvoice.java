package de.kfzteile24.salesOrderHub.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.Date;

@Entity
//@EntityListeners(AuditLogService.class) // WIP
@Table(name = "sales_order_invoice", schema = "public", catalog = "soh")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class SalesOrderInvoice extends AbstractBaseEntity implements AuditableEntity {
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
    @Temporal(TemporalType.TIMESTAMP) //
    private @Nullable
    Date createdAt;

    @Basic
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP) //
    @LastModifiedDate
    private @Nullable
    Date updatedAt;

    @Override
    public String getEntity() {
        return "sales_order";
    }
}
