package com.trading.dto;

import com.trading.entity.Category;
import lombok.Data;

@Data
public class AdminCategoryResponse {
    private Long id;
    private String name;
    private String status;
    private long productCount;

    public static AdminCategoryResponse from(Category c, long productCount) {
        AdminCategoryResponse r = new AdminCategoryResponse();
        r.id = c.getId();
        r.name = c.getName();
        r.status = c.getStatus().name();
        r.productCount = productCount;
        return r;
    }
}
