package de.kfzteile24.salesOrderHub.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.lang.Nullable;

import javax.persistence.*;
import java.util.Date;
import java.util.UUID;

@Entity
@Table(name = "sales_order_address", schema = "public", catalog = "soh")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class SalesOrderAddress extends AbstractBaseEntity {
    // important!
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Basic
    @Column(name = "address_type")
    private String addressType;

    @Basic
    @Column(name = "first_name")
    private String firstName;

    @Basic
    @Column(name = "last_name")
    private String lastName;

    @Basic
    @Column(name = "phone_number")
    private String phoneNumber;

    @Basic
    @Column(name = "company")
    private String company;

    @Basic
    @Column(name = "street1")
    private String street1;

    @Basic
    @Column(name = "street2")
    private String street2;

    @Basic
    @Column(name = "street3")
    private String street3;

    @Basic
    @Column(name = "city")
    private String city;

    @Basic
    @Column(name = "zip_code")
    private String zipCode;

    @Basic
    @Column(name = "tax_number")
    private String taxNumber;

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
}
