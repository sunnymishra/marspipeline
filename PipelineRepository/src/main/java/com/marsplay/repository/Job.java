package com.marsplay.repository;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Document(collection="job")
public class Job {
	@Id
	private String id;
	private String message;
	
	@Field
    @DateTimeFormat(iso = ISO.DATE_TIME)
    private Date createdDate = new Date();
	
	public Job() {}

	public Job(String message, Date createdDate) {
		super();
		this.message = message;
		this.createdDate = createdDate;
	}
	
	public String getId() {
        return id;
    }
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Date getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}
	
	@Override
	public String toString() {
		return "Job [id=" + id + ", message=" + message + ", createdDate="
				+ createdDate + "]";
	}
	
	
}
