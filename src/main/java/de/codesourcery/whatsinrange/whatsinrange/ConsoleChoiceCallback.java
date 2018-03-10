package de.codesourcery.whatsinrange.whatsinrange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.codesourcery.whatsinrange.whatsinrange.HVVScraper.IChoiceCallback;

public class ConsoleChoiceCallback implements IChoiceCallback 
{
    private static final Logger LOG = LogManager.getLogger( ConsoleChoiceCallback.class );

    @Override
    public synchronized Optional<String> makeChoice(POINode node, List<String> choicesFromServer) 
    {
        // remove all addresses
        choicesFromServer.removeIf( s -> s.startsWith("Adresse:" ) );
        
        // if we only have one "Haltestelle" (HS) in the results
        // we'll pick this one automatically
        final Predicate<String> isStop = s -> s.trim().startsWith("HS: ");
        
        final List<String> stops = choicesFromServer.stream().filter( isStop ).collect( Collectors.toList() );
        if ( stops.size() == 1 ) {
            LOG.info("makeChoice(): [AUTO] Picked "+stops.get(0)+" for "+node);            
            return Optional.of( stops.get(0) );
        } 
        else if ( stops.size() > 1 )
        {
            // weed out all stops that do not have the matching type
            final Predicate<String> wrongType;
            if ( node.nodeType == NodeType.SUBWAY_STATION ) { 
                    wrongType = stop -> ! stop.toLowerCase().startsWith("HS: U ");
            } 
            else if ( node.nodeType == NodeType.LIGHT_RAIL_STATION) 
            {
                    wrongType = stop -> ! stop.toLowerCase().startsWith("HS: S ");
            } else {
                wrongType = s -> false;
            }
            stops.removeIf( wrongType );
            if ( stops.size() == 1 ) {
                LOG.info("makeChoice(): [AUTO] Picked "+stops.get(0)+" for "+node);
                return Optional.of( stops.get(0) );
            }
        }
        
        if ( 1 != 2 ) {
            LOG.info("makeChoice(): Too many choices, giving up for "+node);
            return Optional.empty();
        }
        
        final BufferedReader reader = new BufferedReader( new InputStreamReader( System.in ) );
        
        final Function<Integer,String> prefix = index -> {
            String s = "("+index+") ";
            while( s.length() < 6 ) {
                s += " ";
            }
            return s;
        };
        
        while ( true ) 
        {
            System.out.println("Initial origin: "+node);
            int index = 1;
            final int abortIndex=choicesFromServer.size()+1;
            for ( String choice : choicesFromServer ) 
            {
                System.out.println( prefix.apply(index)+choice);
                index++;
            }
            System.out.println( prefix.apply(index)+" SKIP" );
            System.out.println();
            System.out.print("Your choice (1-"+abortIndex+") > ");
            String input;
            try {
                input = reader.readLine();
            } 
            catch (IOException e1) 
            {
                return Optional.empty();
            }
            try {
              input = input.trim();
              System.out.println("User input: "+input);
              final int num = Integer.parseInt( input );
              if ( num >= 1 && num <= abortIndex ) {
                  if ( num == abortIndex ) {
                      return Optional.empty();
                  }
                  return Optional.of( choicesFromServer.get(num-1) );
              }
            } catch(Exception e) {
            }
            System.err.println("Not a valid input: "+input);
        }
    }
};