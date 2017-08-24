package com.marsplay;

import com.marsplay.Constants.Currency;
import com.marsplay.Constants.Endsites;

public class ItemVO {
	private String endsite;
	private String url;
	private String imagePath;
	private String brand;
	private String name;
	private String currency=Currency.INR.toString();
	private Double price;
	private boolean isAvailable=true;
	
	
	public ItemVO(Endsites endsiteName) {
		super();
		this.endsite = endsiteName.toString();
	}
	public String getEndsite() {
		return endsite;
	}
	public void setEndsite(String endsite) {
		this.endsite = endsite;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
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
	public Double getPrice() {
		return price;
	}
	public void setPrice(Double price) {
		this.price = price;
	}
	public boolean isAvailable() {
		return isAvailable;
	}
	public void setAvailable(boolean isAvailable) {
		this.isAvailable = isAvailable;
	}
	public String getImagePath() {
		return imagePath;
	}
	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
	
	@Override
	public String toString() {
		return "ItemVO [endsite=" + endsite + ", url=" + url + ", brand="
				+ brand + ", name=" + name + ", currency=" + currency
				+ ", price=" + price + ", isAvailable=" + isAvailable
				+ ", imagePath=" + imagePath + "]";
	}
	
	
	
	
}
