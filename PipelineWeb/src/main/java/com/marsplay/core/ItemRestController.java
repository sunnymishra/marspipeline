package com.marsplay.core;

import java.net.URI;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.marsplay.repository.Item;
import com.marsplay.repository.ItemRepository;

@RestController
@RequestMapping("/items123")
public class ItemRestController {
	@Autowired
	private ItemRepository itemRepository;

	@RequestMapping(method = RequestMethod.GET)
	public ResponseEntity<?> getTest() {
		System.out.println("Calling getTest API");
		return ResponseEntity.noContent().build();
	}
	
	@RequestMapping(method = RequestMethod.POST)
//	public ResponseEntity<?> add(@PathVariable String accountId,
	public ResponseEntity<?> add(@RequestBody Item input) {
//		this.validateUser(accountId);
		System.out.println("Inside POST ##########################################");
		URI location = null;
//		try {
			Item result = itemRepository.save(input);
//					new Item(input.getName(), input.getDescription()));

			location = ServletUriComponentsBuilder
					.fromCurrentRequest().path("/{id}")
					.buildAndExpand(result.getId()).toUri();

			
		/*} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return ResponseEntity.noContent().build();
		}*/
//		return this.accountRepository
//				.findByUsername(accountId)
//				.map(account -> {
//					Bookmark result = bookmarkRepository.save(new Bookmark(
//							input.getName(), input.getDescription()));
//
//					URI location = ServletUriComponentsBuilder
//							.fromCurrentRequest().path("/{id}")
//							.buildAndExpand(result.getId()).toUri();
//
//					return ResponseEntity.created(location).build();
//				}).orElse(ResponseEntity.noContent().build());
		return ResponseEntity.created(location).build();
	}

/*	@RequestMapping(method = RequestMethod.GET, value = "/{bookmarkId}")
	public Bookmark readBookmark(@PathVariable String accountId,
			@PathVariable String bookmarkId) {
//		this.validateUser(accountId);
		return this.bookmarkRepository.findOne(bookmarkId);
	}*/

/*	private void validateUser(String accountId) {
		this.accountRepository.findByUserName(accountId).orElseThrow(
				() -> new UserNotFoundException(accountId));
	}*/
}
