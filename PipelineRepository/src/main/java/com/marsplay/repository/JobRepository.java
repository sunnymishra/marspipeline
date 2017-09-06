package com.marsplay.repository;

import java.util.Collection;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "job", path = "job")
public interface JobRepository extends MongoRepository<Job, String> {
    Collection<Item> findByCreatedDate(@Param("createdDate") String createdDate);
    
//    Below will fetch value from Referenced Collection
//    Collection<Item> findByBookmarkName(String name);
}

