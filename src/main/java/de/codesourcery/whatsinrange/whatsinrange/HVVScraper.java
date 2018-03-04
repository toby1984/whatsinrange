package de.codesourcery.whatsinrange.whatsinrange;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class HVVScraper extends SearchResultsScraper
{
    private static final Logger LOG = LogManager.getLogger( HVVScraper.class );

	private static final String DESTINATION = "Hauptbahnhof";
			
	private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+):(\\d+)$");
	
	public static void main(String[] args) throws Exception 
	{
		final POINode n = new POINode();
		n.hvvName = "Grundstrasse";
		try ( HVVScraper scraper = new HVVScraper() ) {
		    SearchResult result = scraper.query( n );
		    System.out.println("got: "+result);
		}
	}
	
	public SearchResult query(POINode node) 
	{
        loadPage("search_start", "http://www.hvv.de");
        
        WebElement origin = driver.findElementById("start");
        origin.sendKeys( node.hvvName );
        WebElement destination = driver.findElementById("ziel");
        destination.sendKeys( DESTINATION );
        
        WebElement date = driver.findElementById("datum");
        date.clear();
        date.sendKeys( "05.03.2018" );        
        
        WebElement time = driver.findElementById("uhrzeit");
        time.clear();
        time.sendKeys( "08:00" );
        WebElement arrival = driver.findElementById("ankunft");
        arrival.click();
        WebElement searchButton = driver.findElementByName("suchen");
        searchButton.click();       
        
        LOG.debug("Started to search...");
        
        // wait for results
        driver.findElementByCssSelector("form[action='/jsf/showSearchResult.seam']");
		LOG.debug("Extracting results");
		
		dump("search_results");
		
		final List<WebElement> rows = driver.findElementsByCssSelector("tr[class=\"noborder\"]");
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
		final Optional<Duration> fastest = travelTimes.stream().min( Duration::compareTo );
		return new SearchResult(node.hvvName,DESTINATION,fastest.get() );
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
}