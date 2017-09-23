package com.marsplay.repository;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;

@Document(collection="job")
public class Job {
	@Id
	private String id;
	@Indexed
	private String message;
	private String status;
	
	@Field
    @DateTimeFormat(iso = ISO.DATE_TIME)
    private Date createdDate = new Date();
	
	@Field
    @DateTimeFormat(iso = ISO.DATE_TIME)
    private Date updatedDate = new Date();
	
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
	public Date getUpdatedDate() {
		return updatedDate;
	}
	public void setUpdatedDate(Date updatedDate) {
		this.updatedDate = updatedDate;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public String toString() {
		return "Job [id=" + id + ", message=" + message + ", status=" + status
				+ ", createdDate=" + createdDate + ", updatedDate="
				+ updatedDate + "]";
	}

	

	
}
