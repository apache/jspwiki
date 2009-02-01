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
package org.apache.wiki.ui.stripes;

import java.security.Principal;
import java.util.Collection;
import java.util.Locale;

import org.apache.wiki.auth.WikiPrincipal;

import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;


/**
 * Stripes type converter that converts a Principal name, expressed as a String,
 * into an {@link org.apache.wiki.auth.WikiPrincipal} object. This converter
 * is looked up and returned by the Stripes
 * {@link net.sourceforge.stripes.validation.TypeConverterFactory} for HTTP
 * request parameters that need to be bound to ActionBean properties of type
 * Principal. Stripes executes this TypeConverter during the
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
 * stage of request processing.
 * 
 * @author Andrew Jaquith
 */
public class PrincipalTypeConverter implements TypeConverter<Principal>
{

    /**
     * Converts a named user, passed as a String, into a valid
     * {@link org.apache.wiki.auth.WikiPrincipal} object. This method will not
     * ever return errors.
     * 
     * @param principalName the name of the Principal to create
     * @param targetType the type to return, which will always be of type
     *            {@link java.security.Principal}
     * @param errors the current Collection of validation errors for this field
     * @return the
     */
    public Principal convert( String principalName, Class<? extends Principal> targetType, Collection<ValidationError> errors )
    {
        return new WikiPrincipal( principalName );
    }

    public void setLocale( Locale locale )
    {
    };
}
