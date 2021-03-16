package com.marspipeline.scraper.kafka;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import us.codecraft.xsoup.Xsoup;

public class Test123 {
	public static void main(String[] args) throws IOException {
//		BigDecimal newPrice = formatPrice("\u20B9 329");
//		System.out.println(newPrice);
//		testBreak();
//		readFile();
//		readItemUrl();
		
		try {
			illegal();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			System.out.println("->"+e.getMessage()+"<-");
		}
	}
	public static BigDecimal illegal() {
		throw new IllegalArgumentException("test1212");
	}
	public static BigDecimal formatPrice(String price) {
		String priceTemp = price;
		if (priceTemp == null)
			throw new IllegalArgumentException("Price cannot be null");
		priceTemp = priceTemp.trim();
		if (priceTemp.isEmpty())
			throw new IllegalArgumentException("Price cannot be empty");
		BigDecimal price1;
		System.out.println("priceTemp:"+priceTemp);
		priceTemp = priceTemp.replaceAll("\u20B9", "");
		priceTemp = priceTemp.trim();
		try {
			NumberFormat format = NumberFormat.getInstance(Locale.ENGLISH);
			Number number = format.parse(priceTemp);
			price1 = new BigDecimal(number.toString());
		} catch (NumberFormatException | ParseException e) {
			throw new IllegalArgumentException("Not able to parse price:\""
					+ price + "\"", e);
		}
		return price1;
	}
	
	private static void testBreak(){
		List<List<Integer>> list = Arrays.asList(Arrays.asList(1,2,3), Arrays.asList(4,5,6), Arrays.asList(7,8,9),Arrays.asList(10,11,12),Arrays.asList(13,14,15));
		int counter=0;
		for(List<Integer> innerList: list){
			System.out.println("Reached top of outer loop");
			for(Integer i : innerList){
				counter++;
				System.out.println("Integer:"+i + " counter:"+counter);
				if(counter>=2){
					System.out.println("I am breaking now !!");
					break;
				}
			}
			System.out.println("Reached botton of outer loop");
			if(counter>=2){
				System.out.println("I am breaking now !!");
				break;
			}
		}
	}
	private static void readSponsoredItem() throws IOException{
		File file = new File("D:\\Projects\\Java_Projects\\MarsTomcat\\docs\\html\\AMAZON.jobId.null.html");
		Document document = Jsoup.parse(file, "UTF-8");

		Elements sponsoredElem1 = Xsoup
				.compile("//*[contains(@class, 'sponsored')]")
				.evaluate(document).getElements();
		if (sponsoredElem1.isEmpty()) {
			System.out.println("Blank");
		}
		System.out.println("Not Blank:"+sponsoredElem1);
		
	}
	private static void readItemUrl() throws IOException{
		File file = new File("D:\\Projects\\Java_Projects\\MarsTomcat\\docs\\html\\AMAZON.jobId.1.html");
		Document document = Jsoup.parse(file, "UTF-8");
		System.out.println(document);
		Element li =document.select("li#result_0").first();
		System.out.println(li);
//		String path="div/div/div/div[1]/div/div/span/div/div[1]/a";
		String path="div/div/div/div[1]/div/div/a";
//		String path="div/div/div/div[2]/div[1]/div/a";
		Elements elem1 = Xsoup
				.compile(path)
				.evaluate(li).getElements();
		if (elem1.isEmpty()) {
			System.out.println("Blank");
		}else
			System.out.println("Not Blank:"+elem1);
		
	}
		
		
}
