package com.marsplay.core;

import javax.persistence.Id;

import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import com.fasterxml.jackson.annotation.JsonIgnore;


@Document(collection="item")
public class Item {

//    @DBRef
//    private Bookmark bookmark;

	@Id
    private String id;
    private String name;
    
    private String brand;
    
    @Field("siteUrl")
    @Indexed(unique = true)
    private String siteUrl;
    
    private String imageUrl;
    
	public Item() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Item(String name, String brand, String siteUrl, String imageUrl) {
		super();
		this.name = name;
		this.brand = brand;
		this.siteUrl = siteUrl;
		this.imageUrl = imageUrl;
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

	public String getSiteUrl() {
		return siteUrl;
	}

	public void setSiteUrl(String siteUrl) {
		this.siteUrl = siteUrl;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	@Override
	public String toString() {
		return "Item [id=" + id + ", name=" + name + ", brand=" + brand
				+ ", siteUrl=" + siteUrl + ", imageUrl=" + imageUrl + "]";
	}


}