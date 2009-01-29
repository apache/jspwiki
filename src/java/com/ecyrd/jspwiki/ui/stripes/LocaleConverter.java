/* Copyright 2005-2006 Tim Fennell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.ecyrd.jspwiki.ui.stripes;

import java.util.Collection;
import java.util.Locale;

import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;

import org.apache.commons.lang.StringUtils;

/**
 * Stripes type converter that converts a Locale name, expressed as a String,
 * into an {@link java.util.Locale} object. This converter
 * is looked up and returned by the Stripes
 * {@link net.sourceforge.stripes.validation.TypeConverterFactory} for HTTP
 * request parameters that need to be bound to ActionBean properties of type
 * Locale. Stripes executes this TypeConverter during the
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
 * stage of request processing.
 * 
 * @author Andrew Jaquith
 */
public class LocaleConverter implements TypeConverter<Locale>
{

    /**
     * Converts a named Locale, passed as a String, into a valid
     * {@link java.util.Locale} object. If a Locale cannot be parsed, it
     * will return <code>null</code>. This method will not
     * ever return errors.
     * 
     * @param locale the locale to create
     * @param targetType the type to return, which will always be of type
     *            {@link java.util.Locale}
     * @param errors the current Collection of validation errors for this field
     * @return the
     */
    public Locale convert( String locale, Class<? extends Locale> targetType, Collection<ValidationError> errors )
    {
        if ( locale == null )
        {
            return null;
        }
        
        String language = "";
        String country  = "";
        String variant  = "";
        
        String[] res = StringUtils.split( locale, "-_" );
        
        if( res.length > 2 ) variant = res[2];
        if( res.length > 1 ) country = res[1];
        
        if( res.length > 0 )
        {
            language = res[0];
        
            return new Locale( language, country, variant );
        }
        return null;
    }

    public void setLocale( Locale locale )
    {
    };
}
