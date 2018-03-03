package de.codesourcery.whatsinrange.whatsinrange;

import java.util.Objects;

public final class SelectOption 
{
    public final String rawValue;
    public final boolean isSelected;

    public SelectOption(String value,boolean isSelected) 
    {
        this.rawValue = value;
        this.isSelected = isSelected;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof SelectOption && Objects.equals( this.rawValue , ((SelectOption) obj).rawValue );
    }
    
    @Override
    public int hashCode() {
        return rawValue == null ? 0 : rawValue.hashCode();
    }
    
    public int pageNumber() 
    {
        return Integer.parseInt( rawValue.trim() );
    }
    
    @Override
    public String toString() {
        return rawValue+( isSelected ? "[SELECTED])" : "");
    }
}