package com.kairos.kairosapipostgres.mapper;

import com.kairos.kairosapipostgres.dto.request.ProductRequest;
import com.kairos.kairosapipostgres.dto.response.ProductResponse;
import com.kairos.kairosapipostgres.model.Category;
import com.kairos.kairosapipostgres.model.Product;

public final class ProductMapper {

    private ProductMapper () {
    }

    public static Product toEntity (ProductRequest request, Category category) {
        Product product = new Product ();
        product.setBrand (request.brand().trim());
        product.setPrice (request.price ());
        product.setName (request.name().trim());
        product.setCategory (category);
        return product;
    }

    public static ProductResponse toResponse (Product product) {
        return new ProductResponse (
                product.getId (),
                product.getBrand (),
                product.getPrice (),
                product.getName (),
                CategoryMapper.toResponse(product.getCategory())
        );
    }
}
