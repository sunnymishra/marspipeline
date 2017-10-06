package com.marsplay.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.marsplay.repository.Job;
import com.marsplay.repository.JobRepository;
import com.marsplay.repository.lib.Constants.JobStatus;
import com.marsplay.web.kafka.KafkaSender;
import com.marsplay.web.lib.Util;

@RestController
@RequestMapping("/job")
public class JobRestController {
	private static final Logger LOGGER = LoggerFactory
			.getLogger(JobRestController.class);
	
	@Autowired private JobRepository jobRepository;
	@Autowired private KafkaSender sender;
	@Autowired private MongoTemplate mongoTemplate;
	
	@Value("${kafka.topic.scrape}")
	private String topic;

	/*
	 * @RequestMapping(method = RequestMethod.GET) public ResponseEntity<?>
	 * getTest() { System.out.println("Calling getTest API"); return
	 * ResponseEntity.noContent().build(); }
	 */

	@RequestMapping(method = RequestMethod.POST)
	public ResponseEntity<?> createJob(@RequestBody Job job) {
		long globalStart = System.currentTimeMillis();
		boolean success=false;
		// this.validateUser(accountId);
		LOGGER.info("#######Inside JOB POST.");
		try {
			String msg = job.getMessage();
			if (StringUtils.isEmpty(msg))
				return new ResponseEntity<>("EMPTY_JOB_MESSAGE", HttpStatus.BAD_REQUEST);
			// NOTE: we only permit alphanumeric characters and white space i.e
			// [aA-zZ, 0-9]
			job.setMessage(msg.replaceAll("[^\\w\\s]", ""));
			
			// Below we are checking if a similar Message already exists in JOB collection
			// If yes then return value from DB
			long localStart = System.currentTimeMillis();
			Criteria c = new Criteria().andOperator(Criteria.where("message").is(job.getMessage()),  
                    								Criteria.where("status").is("FINISHED"));
			Job dbJob = mongoTemplate.findOne(Query.query(c), Job.class);
			
//			List<Job> dbJobs=(List<Job>)jobRepository.findByMessage(job.getMessage());
			LOGGER.info(Util.logTime(localStart, "MONGO_FIND_JOB_BY_MESSAGE-STATUS"));
			
			if(dbJob!=null){
				Map<String, String> entity = new HashMap<String, String>();
				entity.put("jobId", dbJob.getId());
				success=true;
				return new ResponseEntity<Object>(entity, HttpStatus.CREATED);
			}
			
			// If NO then create new JOB in DB with status=SUBMITTED
			job.setStatus(JobStatus.SUBMITTED.name());
			
			localStart = System.currentTimeMillis();
			Job result = jobRepository.save(job);
			LOGGER.info(Util.logTime(localStart, "MONGO_SAVE_JOB"));
			
			localStart = System.currentTimeMillis();
			sender.send(topic, result);
			LOGGER.info(Util.logTime(localStart, "KAFKA_SEND"));
			
			Map<String, String> entity = new HashMap<String, String>();
			entity.put("jobId", job.getId());
			success=true;
			return new ResponseEntity<Object>(entity, HttpStatus.OK);

			
//			URI location = null;
//			location = ServletUriComponentsBuilder.fromCurrentRequest()
//			.path("/{id}").buildAndExpand(result.getId()).toUri(); return
//			ResponseEntity.created(location).build();
//			 return this.accountRepository
//			 .findByUsername(accountId)
//			 .map(account -> {
//			 Bookmark result = bookmarkRepository.save(new Bookmark(
//			 input.getName(), input.getDescription()));
//			
//			 URI location = ServletUriComponentsBuilder
//			 .fromCurrentRequest().path("/{id}")
//			 .buildAndExpand(result.getId()).toUri();
//			
//			 return ResponseEntity.created(location).build();
//			 }).orElse(ResponseEntity.noContent().build());

		} catch (Exception e) {
			e.printStackTrace();
			return new ResponseEntity<>(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
//			return ResponseEntity.noContent().build();
		} finally{
			LOGGER.info(Util.logTime(globalStart, "createJob() "+(success?"SUCCESS":"FAILURE")));
		}

	}


	/*
	 * @RequestMapping(method = RequestMethod.GET, value = "/{bookmarkId}")
	 * public Bookmark readBookmark(@PathVariable String accountId,
	 * 
	 * @PathVariable String bookmarkId) { // this.validateUser(accountId);
	 * return this.bookmarkRepository.findOne(bookmarkId); }
	 */

}
