package de.kfzteile24.salesOrderHub.domain;

import lombok.*;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "sales_order_item", schema = "public", catalog = "soh")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class SalesOrderItem extends AbstractBaseEntity {
    // important!
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Basic
    @Column(name = "quantity")
    private BigDecimal quantity;

    @Basic
    @Column(name = "stock_keeping_unit")
    private String stockKeepingUnit;
    @Basic
    @Column(name = "returned_at")
    @Temporal(TemporalType.TIMESTAMP) //
    private @Nullable
    Date returnedAt;

    @Basic
    @Column(name = "delivered_at")
    @Temporal(TemporalType.TIMESTAMP) //
    private @Nullable
    Date deliveredAt;

    @Basic
    @Column(name = "cancellation_at")
    @Temporal(TemporalType.TIMESTAMP) //
    private @Nullable
    Date cancellationAt;

    @Basic
    @Column(name = "shipping_type")
    private String shippingType;

    @Basic
    @Column(name = "tracking_id")
    private String trackingId;
}
