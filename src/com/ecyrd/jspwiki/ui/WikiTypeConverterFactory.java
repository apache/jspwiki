package com.ecyrd.jspwiki.ui;

import java.security.Principal;
import java.util.Locale;

import net.sourceforge.stripes.config.Configuration;
import net.sourceforge.stripes.validation.DefaultTypeConverterFactory;
import net.sourceforge.stripes.validation.TypeConverter;

import com.ecyrd.jspwiki.WikiPage;
import com.ecyrd.jspwiki.auth.authorize.Group;

/**
 * Subclasses Stripes
 * {@link net.sourceforge.stripes.validation.DefaultTypeConverterFactory} and
 * adds support for JSPWiki types such as {@link com.ecyrd.jspwiki.WikiPage}.
 * This implementation differs from the DefaultTypeConverterFactory in only one
 * respect: the overridden {@link #init(Configuration)} method merely adds
 * converters for JSPWiki types after normal initialization.
 * 
 * @author Andrew Jaquith
 */
public class WikiTypeConverterFactory extends DefaultTypeConverterFactory
{
    /**
     * @param configuration
     *            the Stripes configuration, which must be of type
     *            {@link WikiRuntimeConfiguration}.
     * @throws IllegalArgumentException
     *             if configuration is not of type WikiRuntimeConfiguration
     */
    @Override
    public void init(Configuration configuration)
    {
        super.init(configuration);
        
        // Add our custom converters
        super.add(Group.class, GroupTypeConverter.class);
        super.add(Principal.class, PrincipalTypeConverter.class);
        super.add(WikiPage.class, WikiPageTypeConverter.class);
    }

    @Override
    public TypeConverter getInstance(Class<? extends TypeConverter> arg0, Locale arg1) throws Exception
    {
        TypeConverter converter = super.getInstance(arg0, arg1);
        return converter;
    }

    @Override
    public TypeConverter getTypeConverter(Class forType, Locale locale) throws Exception
    {
        TypeConverter converter = super.getTypeConverter(forType, locale);
        return converter;
    }
    
}
