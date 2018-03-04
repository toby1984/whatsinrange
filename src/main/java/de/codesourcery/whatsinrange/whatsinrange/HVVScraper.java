package de.codesourcery.whatsinrange.whatsinrange;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.springframework.stereotype.Component;

@Component
public class HVVScraper extends SearchResultsScraper 
{
	private static final String DESTINATION = "Hauptbahnhof";
			
	private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+):(\\d+)$");
	
	private POINode node;

	public static void main(String[] args) {
		POINode n = new POINode();
		n.hvvName = "Grundstrasse";
		HVVScraper scraper = new HVVScraper(n);
		scraper.performSearch();
	}
	
	public HVVScraper(POINode node) {
		this.node = node;
	}
	
	@Override
	protected String getSearchURL() {
		return "http://www.hvv.de/";
	}
	
	@Override
	protected List<SearchResult> extractSearchResults() {
		
		System.out.println("Extracting results");
		dump("search_results");
		List<WebElement> rows = driver.findElementsByCssSelector("tr[class=\"noborder\"]");
		final List<Duration> travelTimes = new ArrayList<>();
		for ( WebElement row : rows ) 
		{
			List<WebElement> children = row.findElements( By.xpath("*") );
			final Predicate<WebElement> predicate = c -> "Reisezeit (Start - Ziel)".equals( c.getText() );
			final boolean isMatch = children.stream().anyMatch( predicate );
			if ( isMatch ) 
			{
			  children.stream().filter( c -> ! predicate.test(c) ).forEach( c -> travelTimes.add( parseDuration(c.getText()) ) );	
			}
		}
		Optional<Duration> fastest = travelTimes.stream().min( Duration::compareTo );
		SearchResult result = new SearchResult();
		result.origin = node.hvvName;
		result.destination = DESTINATION;
		result.timeInMinutes = fastest.get().getSeconds()/60;
		System.out.println("Travel times: "+travelTimes);
		return new ArrayList<>();
	}
	
	private static Duration parseDuration(String s) {
		if ( s == null || s.trim().isEmpty() ) {
			throw new IllegalArgumentException("Duration string is NULL/blank?");
		}
		Matcher m = DURATION_PATTERN.matcher( s.trim() );
		if ( ! m.matches() ) {
			throw new IllegalArgumentException("Unrecognized duration: >"+s+"<");
		}
		final int hours = Integer.parseInt( m.group(1) );
		final int minutes = Integer.parseInt( m.group(2) );
		return Duration.ofMinutes( minutes ).plus( Duration.ofHours( hours ) );
	}

	@Override
	protected void loadFirstResultPage() {
		loadPage("search_start", "http://www.hvv.de");
		
		WebElement origin = driver.findElementById("start");
		origin.sendKeys( node.hvvName );
		WebElement destination = driver.findElementById("ziel");
		destination.sendKeys( DESTINATION );
		WebElement time = driver.findElementById("uhrzeit");
		time.clear();
		time.sendKeys( "08:00" );
		WebElement arrival = driver.findElementById("ankunft");
		arrival.click();
		WebElement searchButton = driver.findElementByName("suchen");
		searchButton.click();		
		System.out.println("Started to search...");
		try { Thread.sleep(5000); } catch(Exception e) {}
	}

	@Override
	protected Pagination getPagination() {
		return new Pagination(new ArrayList<>()) {

			@Override
			public void select(SelectOption option) {
			}
		};
	}
}