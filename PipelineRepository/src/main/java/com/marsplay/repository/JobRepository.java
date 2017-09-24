package com.marsplay.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel = "job", path = "job")
public interface JobRepository extends MongoRepository<Job, String> {
	Optional<Job> findById(@Param("jobId") String jobId);
    Collection<Job> findByCreatedDate(@Param("createdDate") String createdDate);
//    Collection<Job> findByMessage(String message);
}

