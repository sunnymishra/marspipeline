package com.marsplay.core;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "item", path = "item")
public interface ItemRepository extends MongoRepository<Item, String> {
    Collection<Item> findByBrand(String brand);
    Optional<Item> findByImageUrl(String imageUrl);
    
//    Collection<Item> findByBookmarkName(String name);
}