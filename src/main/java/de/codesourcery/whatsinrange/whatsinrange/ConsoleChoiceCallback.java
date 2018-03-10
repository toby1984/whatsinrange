package de.codesourcery.whatsinrange.whatsinrange;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import de.codesourcery.whatsinrange.whatsinrange.HVVScraper.IChoiceCallback;

public class ConsoleChoiceCallback implements IChoiceCallback 
{
    @Override
    public synchronized Optional<String> makeChoice(POINode node, List<String> choicesFromServer) 
    {
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