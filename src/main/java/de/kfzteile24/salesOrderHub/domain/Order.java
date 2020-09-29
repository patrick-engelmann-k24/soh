package de.kfzteile24.salesOrderHub.domain;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;
import org.hibernate.id.enhanced.SequenceStyleGenerator;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "order")
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Data
@Builder
@EqualsAndHashCode()
@ToString()
public class Order {

    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "id", nullable = false)
    @GenericGenerator(
            name = "tableSequenceGenerator",
            strategy = "org.hibernate.id.enhanced.SequenceStyleGenerator",
            parameters = {
                    @Parameter(name = SequenceStyleGenerator.OPT_PARAM, value = "noop"),
                    @Parameter(name = SequenceStyleGenerator.SEQUENCE_PARAM, value = "seq_global"),
                    @Parameter(name = SequenceStyleGenerator.INITIAL_PARAM, value = "1"),
                    @Parameter(name = SequenceStyleGenerator.INCREMENT_PARAM, value = "7")
            }
    )
    @GeneratedValue(generator = "tableSequenceGenerator")
    private Integer id;

    @Basic
    @Column(name = "orderNumber", length = 128)
    private String orderNumber;

    @Basic
    @Column(name = "orderDateTime")
    @Temporal(TemporalType.TIMESTAMP)
    private Date orderDateTime;



}
