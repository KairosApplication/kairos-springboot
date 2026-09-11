package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.CategoryRequest;
import com.kairos.kairosapipostgres.dto.response.CategoryResponse;
import com.kairos.kairosapipostgres.model.Category;

public final class CategoryMapper {

    private CategoryMapper () {
    }

    public static Category toEntity (CategoryRequest request) {
        Category category = new Category ();
        category.setCategory (request.category ());
        return category;
    }

    public static CategoryResponse toResponse (Category category) {
        return new CategoryResponse (
                category.getId (),
                category.getCategory ()
        );
    }
}