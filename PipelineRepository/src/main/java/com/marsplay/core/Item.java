package com.marsplay.core;

import java.math.BigDecimal;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;


@Document(collection="item")
public class Item {

//    @DBRef
//    private Bookmark bookmark;

	@Id
    private String id;
    private String name;
    private String brand;
    private String endSite;
    @Field("siteUrl")
    @Indexed(unique = true)
    private String siteUrl;
    
    private String imageUrl;
    private BigDecimal price;	// Wanted to try Decimal128
    
	public Item() {
		super();
		// TODO Auto-generated constructor stub
	}

	public Item(String name, String brand, String endSite, String siteUrl, String imageUrl, BigDecimal price) {
		super();
		this.name = name;
		this.brand = brand;
		this.endSite = endSite;
		this.siteUrl = siteUrl;
		this.imageUrl = imageUrl;
		this.price = price;
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

	@Override
	public String toString() {
		return "Item [id=" + id + ", name=" + name + ", brand=" + brand
				+ ", endSite=" + endSite + ", siteUrl=" + siteUrl
				+ ", imageUrl=" + imageUrl + ", price=" + price + "]";
	}

	


}