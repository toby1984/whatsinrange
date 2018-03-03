package de.codesourcery.whatsinrange.whatsinrange;

import java.util.List;
import java.util.Optional;

public abstract class Pagination 
{
    private final List<SelectOption> options;
    
    public Pagination(List<SelectOption> options)
    {
        this.options = options;
        options.sort( (a,b) -> Integer.compare( a.pageNumber() , b.pageNumber() ) );
    }
    
    public boolean isOnLastPage() 
    {
        return options.size() <= 1 || currentPage().get().pageNumber() == lastPage().get().pageNumber();
    }
    
    public boolean hasNextPage() {
        return currentPage().isPresent() && ! isOnLastPage() && nextPage( currentPage().get() ).isPresent();
    }
    
    public boolean selectNextPage() 
    {
        if ( hasNextPage() )
        {
            select( nextPage( currentPage().get() ).get() );
            return true;
        }
        return false;
    }
    
    public Optional<SelectOption> lastPage() {
        return options.isEmpty() ? Optional.empty() : Optional.of( options.get( options.size()-1 ) );
    }
    
    public Optional<SelectOption> nextPage(SelectOption option) 
    {
        int idx = options.indexOf( option );
        if ( idx == -1 ) {
            throw new IllegalArgumentException("Element "+option+" not found in this pagination");
        }
        if ( idx == options.size()-1 ) {
            return Optional.empty();
        }
        return Optional.of( options.get( idx+1 ) );
    }
    
    public abstract void select(SelectOption option);
    
    public Optional<SelectOption> currentPage() {
        return options.stream().filter( opt -> opt.isSelected ).findFirst();
    }

    @Override
    public String toString() {
        return "Selected: "+currentPage()+" / last: "+lastPage();
    }
}