package de.codesourcery.whatsinrange.whatsinrange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

public class HVVScraper extends SearchResultsScraper
{
    private static final Logger LOG = LogManager.getLogger( HVVScraper.class );

	private static final String DESTINATION = "Hauptbahnhof";
			
	private static final Pattern DURATION_PATTERN = Pattern.compile("^(\\d+):(\\d+)$");
	
    public interface IChoiceCallback 
    {
        public Optional<String> makeChoice(POINode node,List<String> choicesFromServer); 
    }	
	
	public static void main(String[] args) throws Exception 
	{
		final POINode n = new POINode();
		n.hvvName = "Heidkoppelweg";
		try ( HVVScraper scraper = new HVVScraper() {
		    @Override
		    protected PhantomJSDriver createDriver() 
		    {
		        setTimeout( Duration.ofSeconds( 5 ) );
		        return super.createDriver();
		    }
		}) 
		{
		    scraper.setSavePages( true );
		    SearchResult result = scraper.query( n, new ConsoleChoiceCallback() );
		    System.out.println("got: "+result);
		}
	}
	
	private ZonedDateTime dateToQuery;
	
	public HVVScraper() 
	{
	    final int hourToQuery = 8;
	    final int minuteToQuery = 0;
	    ZonedDateTime nextMonday = ZonedDateTime.now().withZoneSameLocal(ZoneId.of("Europe/Berlin"));
	    while ( nextMonday.getDayOfWeek() != DayOfWeek.MONDAY || nextMonday.isBefore( ZonedDateTime.now() ) ) {
	        nextMonday = nextMonday.plusDays( 1 ); 
	    }
	    dateToQuery = nextMonday.withHour( hourToQuery ).withMinute( minuteToQuery ).withSecond( 0 ).withNano( 0 );
	}
	
	public SearchResult query(POINode node,IChoiceCallback callback)
	{
        loadPage("search_start", "http://www.hvv.de");
        
        WebElement origin = driver.findElementById("start");
        origin.sendKeys( node.hvvName );
        WebElement destination = driver.findElementById("ziel");
        destination.sendKeys( DESTINATION );
        
        final String dateString = DateTimeFormatter.ofPattern("dd.MM.yyyy").format( dateToQuery );
        final String timeString = DateTimeFormatter.ofPattern("HH:mm").format( dateToQuery );
        
        WebElement date = driver.findElementById("datum");
        date.clear();
        date.sendKeys( dateString );        
        
        WebElement time = driver.findElementById("uhrzeit");
        time.clear();
        time.sendKeys( timeString );
        WebElement arrival = driver.findElementById("ankunft");
        arrival.click();
        WebElement searchButton = driver.findElementByName("suchen");
        searchButton.click();       
        
        LOG.debug("Started to search...");
        
        // wait for results
        try {
        	driver.findElementByCssSelector("form[action='/jsf/showSearchResult.seam']");
        } 
        catch(org.openqa.selenium.NoSuchElementException e) 
        {
            LOG.info("failed to find results form, checking for multiple inputs");
            try 
            {
                final List<WebElement> options= driver.findElementsByCssSelector("select[id='personalSearch:startSelectMenu'] option");
                if ( options.isEmpty() ) {
                    throw new RuntimeException("Found no options for "+node);
                }
                final List<String> choices = options.stream().map( element -> element.getAttribute("innerText") ).collect( Collectors.toList() );
                final Optional<String> userChoice = callback.makeChoice( node, choices );
                if ( ! userChoice.isPresent() || node.hvvName.equalsIgnoreCase( userChoice.get() ) ) {
                    LOG.warn("query(): No user choice for "+node);
                    return null;
                }
                LOG.info("query(): Retrying search with user choice "+userChoice.get()+" for "+node);
                node.hvvName = userChoice.get();
                return query( node , callback );
            } catch(org.openqa.selenium.NoSuchElementException e2) {
                dump("search_results");
                LOG.error("query(): Expected page with options but got something else for "+node);
                return null;
            }
        }
		LOG.debug("Extracting results");
		
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