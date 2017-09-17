package com.marsplay.web;

import java.net.URI;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.marsplay.repository.Job;
import com.marsplay.repository.JobRepository;
import com.marsplay.repository.lib.Constants.JobStatus;
import com.marsplay.web.kafka.KafkaSender;

@RestController
@RequestMapping("/job")
public class JobRestController {
	@Autowired
	private JobRepository jobRepository;

	@Autowired
	private KafkaSender sender;

	@Value("${kafka.topic.scrape}")
	private String topic;

	/*@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<?> getTest() {
		System.out.println("Calling getTest API");
		return ResponseEntity.noContent().build();
	}*/

	@RequestMapping(method = RequestMethod.POST)
	// public ResponseEntity<?> add(@PathVariable String accountId,
	public ResponseEntity<?> add(@RequestBody Job input) {
		// this.validateUser(accountId);
		System.out
				.println("Inside POST ##########################################");
		URI location = null;
		// try {
		input.setStatus(JobStatus.SUBMITTED.name());
		Job result = jobRepository.save(input);

		sender.send(topic, result);

		location = ServletUriComponentsBuilder.fromCurrentRequest()
				.path("/{id}").buildAndExpand(result.getId()).toUri();

		/*
		 * } catch (Exception e) { // TODO Auto-generated catch block
		 * e.printStackTrace(); return ResponseEntity.noContent().build(); }
		 */
		// return this.accountRepository
		// .findByUsername(accountId)
		// .map(account -> {
		// Bookmark result = bookmarkRepository.save(new Bookmark(
		// input.getName(), input.getDescription()));
		//
		// URI location = ServletUriComponentsBuilder
		// .fromCurrentRequest().path("/{id}")
		// .buildAndExpand(result.getId()).toUri();
		//
		// return ResponseEntity.created(location).build();
		// }).orElse(ResponseEntity.noContent().build());
		return ResponseEntity.created(location).build();
	}

	/*
	 * @RequestMapping(method = RequestMethod.GET, value = "/{bookmarkId}")
	 * public Bookmark readBookmark(@PathVariable String accountId,
	 * 
	 * @PathVariable String bookmarkId) { // this.validateUser(accountId);
	 * return this.bookmarkRepository.findOne(bookmarkId); }
	 */

}
