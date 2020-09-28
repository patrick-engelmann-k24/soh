package de.kfzteile24.salesOrderHub.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "sales_order", schema = "public", catalog = "soh")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class SalesOrder extends AbstractBaseEntity {

    private static final long serialVersionUID = 1L;

    @Basic
    @Column(name = "order_number")
    private String orderNumber;

    @Basic
    @Column(name = "process_id")
    private UUID processId;

//    @Basic
//    @Column(name = "original_order")
//    private String originalOrder;

    @Basic
    @Column(name = "customer_email")
    private String customerEmail;

    @Basic
    @Column(name = "customer_number")
    private String customerNumber;

    @Basic
    @Column(name = "sales_channel")
    private String salesChannel;

    @Basic
    @Column(name = "sales_locale")
    private String salesLocale;

    @Basic
    @Column(name = "offer_reference_number")
    private String offerReferenceNumber;

    @Basic
    @Column(name = "created_at")
    @CreatedDate
    @Temporal(TemporalType.TIMESTAMP) //
    private @Nullable
    Date createdAt;

    @Basic
    @Column(name = "updated_at")
    @LastModifiedDate
    @Temporal(TemporalType.TIMESTAMP) //
    private @Nullable
    Date updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "salesOrder", targetEntity = SalesOrderItem.class, fetch = FetchType.LAZY)
    private Set<SalesOrderItem> salesOrderItemList;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "salesOrder", targetEntity = SalesOrderInvoice.class, fetch = FetchType.LAZY)
    private Set<SalesOrderInvoice> salesOrderInvoiceList;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "salesOrder", targetEntity = SalesOrderAddress.class, fetch = FetchType.LAZY)
    private Set<SalesOrderAddress> salesOrderAddressList;
}
