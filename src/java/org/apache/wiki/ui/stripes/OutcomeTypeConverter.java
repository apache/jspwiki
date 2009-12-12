/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */
package org.apache.wiki.ui.stripes;

import java.util.Collection;
import java.util.Locale;

import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;

import org.apache.wiki.workflow.NoSuchOutcomeException;
import org.apache.wiki.workflow.Outcome;

/**
 * Stripes type converter that converts an Outcome, expressed as a String,
 * into an {@link Outcome} object. This converter
 * is looked up and returned by the Stripes
 * {@link net.sourceforge.stripes.validation.TypeConverterFactory} for HTTP
 * request parameters that need to be bound to ActionBean properties of type
 * Outcome. Stripes executes this TypeConverter during the
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
 * stage of request processing.
 * 
 */
public class OutcomeTypeConverter implements TypeConverter<Outcome>
{

    /**
     * Converts a named Outcome, passed as a String, into a valid
     * {@link Outcome} object. The Outcome is looked up by passing
     * {@code outcomeKey} to {@link Outcome#forName(String)}.
     * If the Outcome cannot be found (perhaps because it does not exist), this
     * method will add a validation error to the supplied Collection of errors.
     * The error will be of type
     * {@link net.sourceforge.stripes.validation.LocalizableError} and will have
     * a message key of <code>outcome.doesnotexist</code> with parameter
     * <code>{0}</code> (equal to the value passed for <code>outcomeKey</code>).
     * If a Outcome cannot be parsed or looked up, this method returns
     * <code>null</code>. This method will not ever return errors.
     * 
     * @param outcomeKey the key representing the Outcome to look up
     * @param targetType the type to return, which will always be of type
     *            {@link Outcome}
     * @param errors the current Collection of validation errors for this field
     * @return the Outcome
     */
    public Outcome convert( String outcomeKey, Class<? extends Outcome> targetType, Collection<ValidationError> errors )
    {
        if ( outcomeKey == null )
        {
            return null;
        }
        
        Outcome outcome = null;
        try
        {
            outcome = Outcome.forName( outcomeKey );
        }
        catch( NoSuchOutcomeException e )
        {
            // Illegal Outcome
            errors.add( new LocalizableError( "outcome.doesnotexist" ) );
        }
        return outcome;
    }

    /**
     * No-op method that does nothing, because setting the Locale has no effect on the conversion.
     */
    public void setLocale( Locale locale )
    {
    };
}
