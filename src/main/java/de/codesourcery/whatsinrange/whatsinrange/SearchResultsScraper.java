package de.codesourcery.whatsinrange.whatsinrange;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

public abstract class SearchResultsScraper implements AutoCloseable 
{
    private static final Logger LOG = LogManager.getLogger( SearchResultsScraper.class );

    private boolean savePages = false;
    private Duration timeout = Duration.ofSeconds( 10 );

    protected PhantomJSDriver driver;   

    public SearchResultsScraper() {
        setDriver( createDriver() );
    }
    
    protected PhantomJSDriver createDriver() 
    {
        final DesiredCapabilities caps = createCapabilities();
        final PhantomJSDriver driver = new PhantomJSDriver(caps);
        driver.manage().timeouts().implicitlyWait( timeout.getSeconds() , TimeUnit.SECONDS);
        return driver;
    } 
    
    public void setTimeout(Duration timeout) {
        this.timeout = timeout;
    }
    
    @Override
    public void close() throws Exception {
        driver.quit();
    }
    
    public SearchResultsScraper(PhantomJSDriver driver) {
        this.driver = driver;
    }
    
    protected final void setDriver(PhantomJSDriver driver) {
        this.driver = driver;
    }

    protected DesiredCapabilities createCapabilities() 
    {
        final DesiredCapabilities caps = new DesiredCapabilities();
        caps.setJavascriptEnabled(true);                
        caps.setCapability("takesScreenshot", true);
        
        String executablePath = System.getProperty("phantomjs.executable");
        if ( StringUtils.isBlank(executablePath) ) {
            executablePath = "/home/tobi/apps/phantomjs/bin/phantomjs"; 
        }
        LOG.info("PhantomJS executable: "+executablePath);
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_EXECUTABLE_PATH_PROPERTY, executablePath );

        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "userAgent", "Mozilla/5.0 (X11; Linux x86_64; rv:54.0) Gecko/20100101 Firefox/54.0");
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "Accept-Language", "en-US,en;q=0.5");
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "DNT", "1");
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_CUSTOMHEADERS_PREFIX + "Upgrade-Insecure-Requests", "1");
        return caps;
    }

    protected final void loadPage(String debugName, String url) 
    {
        LOG.info("Loading "+debugName+"...");
        driver.get( url );
        LOG.info("Done loading "+debugName);

        if ( savePages ) {
            dump(debugName);
        }
    }

    protected final void time(String msg,Runnable r) 
    {
        System.out.println(msg+"...");
        long time1 = System.currentTimeMillis();
        r.run();
        long time2 = System.currentTimeMillis();
        System.out.println("done! ("+(time2-time1+") ms"));
    }

    protected final void dump(String fileNamePrefix) {

        if ( ! savePages ) {
            return;
        }
        
        final String pageSource = driver.getPageSource();

        int index = 1;
        final String baseDir = "/tmp";
        String diff = "";
        String suffix= ".html";

        do {
            File file = new File( baseDir+"/"+fileNamePrefix+diff+suffix );
            if ( ! file.exists() ) 
            {
                System.out.println("Saving page to "+file.getAbsolutePath());                
                try ( Writer out = new FileWriter( file ) ) {
                    out.write( pageSource );
                } 
                catch (IOException e) 
                {
                    System.err.println("Failed to save page source");
                }
                return;
            }
            diff = "_"+index;
            index++;
        } while ( true );
    }

    protected final String resolveRelativeURL(String url) 
    {
        final String current = driver.getCurrentUrl();
        try {
            return new URI( current ).resolve( url ).toString();
        } catch (URISyntaxException e) {
            LOG.error("resolveURL(): current="+current+" | rel: "+url,e);
            throw new RuntimeException(e);
        }
    }

    protected static <T> Stream<T> streamopt(Optional<T> opt) 
    {
        return opt.isPresent() ? Stream.of(opt.get()) :  Stream.empty();
    }

    protected static String cleanString(String input) 
    {
        if ( input == null || input.trim().length() == 0 ) {
            return input;
        }
        System.out.println("CLEANING: >"+input+"<");
        StringBuilder buffer = new StringBuilder();
        int i = 0;
        while( Character.isWhitespace( input.charAt( i ) ) ) {
            i++;
        }
        char previousChar = input.charAt( i++ );
        buffer.append( previousChar );

        for ( ; i < input.length() ; i++ ) 
        {
            final char c = input.charAt( i );
            if ( c >= 32 && (previousChar != c || ! Character.isWhitespace( c ) ) ) 
            {
                buffer.append( c );    
            } 
            previousChar = c;
        }
        String result = buffer.toString();
        result = result.replaceAll("<!--.*?-->" , "" );
        result = result.replaceAll("<[a-zA-Z]+( .*?){0,1}>.*?</[a-zA-Z]+>" , "" );
        return result;
    }
    
    public final void setSavePages(boolean savePages) {
        this.savePages = savePages;
    }
    
    protected final String getText(WebElement element) 
    {
        Object r = driver.executeScript("return arguments[0].innerHTML;" , element );
        return cleanString( r == null ? null : r.toString() );
    }
}