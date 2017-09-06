package com.marsplay.repository;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;


@Document(collection="item")
public class Item {

	@Id
    private String id;
	
	@Field("endsiteUrl")
    @Indexed(unique = true)
    private String endsiteUrl;
    
    @DBRef
    private Job job;
    
    private String name;
    private String brand;
    private String endSite;
    private String endsiteImageUrl;
    private String cdnImageUrl;
    private String cdnImageId;
    private BigDecimal price;	// Wanted to try Decimal128
    
    @Field
    @DateTimeFormat(iso = ISO.DATE_TIME)
    private Date createdDate = new Date();
    
	public Item() {}

	public Item(String endsiteUrl, Job job, String name, String brand, String endSite, 
			String endsiteImageUrl, String cdnImageUrl, String cdnImageId,
			BigDecimal price, Date createdDate) {
		super();
		this.endsiteUrl = endsiteUrl;
		this.job = job;
		this.name = name;
		this.brand = brand;
		this.endSite = endSite;
		this.endsiteImageUrl = endsiteImageUrl;
		this.cdnImageUrl = cdnImageUrl;
		this.cdnImageId = cdnImageId;
		this.price = price;
		this.createdDate = createdDate;
	}

	public String getId() {
        return id;
    }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getBrand() {
		return brand;
	}

	public void setBrand(String brand) {
		this.brand = brand;
	}

	public String getEndsiteUrl() {
		return endsiteUrl;
	}

	public void setEndsiteUrl(String endsiteUrl) {
		this.endsiteUrl = endsiteUrl;
	}


	public String getCdnImageUrl() {
		return cdnImageUrl;
	}

	public void setCdnImageUrl(String cdnImageUrl) {
		this.cdnImageUrl = cdnImageUrl;
	}

	public String getEndSite() {
		return endSite;
	}

	public void setEndSite(String endSite) {
		this.endSite = endSite;
	}

	public BigDecimal getPrice() {
		return price;
	}

	public void setPrice(BigDecimal price) {
		this.price = price;
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public String getEndsiteImageUrl() {
		return endsiteImageUrl;
	}

	public void setEndsiteImageUrl(String endsiteImageUrl) {
		this.endsiteImageUrl = endsiteImageUrl;
	}

	public String getCdnImageId() {
		return cdnImageId;
	}

	public void setCdnImageId(String cdnImageId) {
		this.cdnImageId = cdnImageId;
	}

	public Job getJob() {
		return job;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	@Override
	public String toString() {
		return "Item [id=" + id + ", endsiteUrl=" + endsiteUrl + ", job=" + job
				+ ", name=" + name + ", brand=" + brand + ", endSite="
				+ endSite + ", endsiteImageUrl=" + endsiteImageUrl
				+ ", cdnImageUrl=" + cdnImageUrl + ", cdnImageId=" + cdnImageId
				+ ", price=" + price + ", createdDate=" + createdDate + "]";
	}




	


}