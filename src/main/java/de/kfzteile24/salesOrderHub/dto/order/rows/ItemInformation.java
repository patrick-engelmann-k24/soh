package de.kfzteile24.salesOrderHub.dto.order.rows;

import lombok.Data;

@Data
public class ItemInformation {
    private String imageUrl;
    private String thumbnailUrl;
    private String brand;
    private Boolean isBulkyGood;
    private Boolean isRiskyGood;
    private Boolean isHazardousGood;
    private String legalNotice;
    private Boolean pricehammer;
    private ArticleInfo offer;
    private ArticleInfo order;
    private ArticleInfo invoice;
}
