package com.marspipeline.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "item", path = "item")
public interface ItemRepository extends MongoRepository<Item, String> {
    Collection<Item> findByBrand(@Param("brand")String brand);
    Optional<Item> findByEndsiteUrl(@Param("endsiteUrl")String endsiteUrl);
//  Below method will fetch value from Referenced Collection "JOB"
    Collection<Item> findByJobId(@Param("jobId")String jobId);
}

