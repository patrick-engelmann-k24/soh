package de.kfzteile24.salesOrderHub.domain;

import de.kfzteile24.salesOrderHub.domain.converter.OrderJsonConverter;
import de.kfzteile24.salesOrderHub.dto.OrderJSON;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

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
    private String processId;

    @Column(name = "original_order", columnDefinition = "json")
    @Convert(converter = OrderJsonConverter.class)
    private OrderJSON originalOrder;

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
    private LocalDateTime createdAt;

    @Basic
    @Column(name = "updated_at")
    @LastModifiedDate
    private LocalDateTime updatedAt;

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
