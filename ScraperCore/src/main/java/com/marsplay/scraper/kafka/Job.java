package com.marsplay.scraper.kafka;

import java.util.Date;

public class Job {
	String jobId;
	String message;
	Date created;
	
	public Job() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Job(String jobId, String message, Date created) {
		super();
		this.jobId = jobId;
		this.message = message;
		this.created = created;
	}
	
	public String getJobId() {
		return jobId;
	}
	public void setJobId(String jobId) {
		this.jobId = jobId;
	}
	public String getMessage() {
		return message;
	}
	public void setMessage(String message) {
		this.message = message;
	}
	public Date getCreated() {
		return created;
	}
	public void setCreated(Date created) {
		this.created = created;
	}
	
	@Override
	public String toString() {
		return "Job [jobId=" + jobId + ", message=" + message + ", created="
				+ created + "]";
	}
	
	
}
