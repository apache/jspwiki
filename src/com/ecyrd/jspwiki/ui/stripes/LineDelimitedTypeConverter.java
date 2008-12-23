package com.ecyrd.jspwiki.ui.stripes;

import net.sourceforge.stripes.validation.OneToManyTypeConverter;

/**
 * Overrides {@link net.sourceforge.stripes.validation.OneToManyTypeConverter} so
 * that multiple items parsed by the converter use carriage return delimiters, instead of spaces and commas.
 */
public class LineDelimitedTypeConverter extends OneToManyTypeConverter
{

    @Override
    protected String getSplitRegex()
    {
        return "\n";
    }
    
}
