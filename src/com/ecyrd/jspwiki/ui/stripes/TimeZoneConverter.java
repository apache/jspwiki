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
import java.util.TimeZone;

import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;

/**
 * Stripes type converter that converts a {@link java.util.TimeZone}, expressed
 * as a String, into a TimeZone object. This converter is looked up and returned
 * by the Stripes
 * {@link net.sourceforge.stripes.validation.TypeConverterFactory} for HTTP
 * request parameters that need to be bound to ActionBean properties of type
 * TimeZone. Stripes executes this TypeConverter during the
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
 * stage of request processing.
 * 
 * @author Andrew Jaquith
 */
public class TimeZoneConverter implements TypeConverter<TimeZone>
{

    /**
     * Converts a named TimeZone, passed as a String, into a valid
     * {@link java.util.TimeZone} object. If a TimeZone cannot be parsed, it will
     * return <code>null</code>. This method will not ever return errors.
     * 
     * @param timeZone  the TimeZone  to create; must be equivalent to {@link java.util.TimeZone#getID()}
     * @param targetType the type to return, which will always be of type
     *            {@link java.util.Locale}
     * @param errors the current Collection of validation errors for this field
     * @return the time zone
     */
    public TimeZone convert( String timeZone, Class<? extends TimeZone> targetType, Collection<ValidationError> errors )
    {
        return TimeZone.getTimeZone( timeZone );
    }

    public void setLocale( Locale locale )
    {
    };
}
