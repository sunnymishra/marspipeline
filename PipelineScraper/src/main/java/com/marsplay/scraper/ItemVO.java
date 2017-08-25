package com.marsplay.scraper;

import java.math.BigDecimal;

import com.marsplay.scraper.Constants.Currency;
import com.marsplay.scraper.Constants.Endsites;

public class ItemVO {
	private String endSite;
	private String siteUrl;
	private String imageUrl;
	private String brand;
	private String name;
	private String currency=Currency.INR.toString();
	private BigDecimal price;
	private boolean isAvailable=true;
	
	
	public ItemVO(Endsites endsiteName) {
		super();
		this.endSite = endsiteName.toString();
	}
	public String getEndSite() {
		return endSite;
	}
	public void setEndSite(String endsite) {
		this.endSite = endsite;
	}
	public String getSiteUrl() {
		return siteUrl;
	}
	public void setSiteUrl(String url) {
		this.siteUrl = url;
	}
	public String getBrand() {
		return brand;
	}
	public void setBrand(String brand) {
		this.brand = brand;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public BigDecimal getPrice() {
		return price;
	}
	public void setPrice(BigDecimal price) {
		this.price = price;
	}
	public boolean isAvailable() {
		return isAvailable;
	}
	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}
	public String getImageUrl() {
		return imageUrl;
	}
	public void setImageUrl(String imagePath) {
		this.imageUrl = imagePath;
	}
	
	@Override
	public String toString() {
		return "ItemVO [endsite=" + endSite + ", url=" + siteUrl + ", brand="
				+ brand + ", name=" + name + ", currency=" + currency
				+ ", price=" + price + ", isAvailable=" + isAvailable
				+ ", imagePath=" + imageUrl + "]";
	}
	
	
	
	
}
