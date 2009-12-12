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

import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.authorize.Group;
import org.apache.wiki.auth.authorize.GroupManager;

import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;


/**
 * Stripes type converter that converts a Group name, expressed as a String,
 * into an {@link org.apache.wiki.auth.authorize.Group} object. This converter
 * is looked up and returned by the Stripes
 * {@link net.sourceforge.stripes.validation.TypeConverterFactory} for HTTP
 * request parameters that need to be bound to ActionBean properties of type
 * Group. Stripes executes this TypeConverter during the
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
 * stage of request processing.
 * 
 */
public class GroupTypeConverter implements TypeConverter<Group>
{
    /**
     * Converts a named wiki group into a valid
     * {@link org.apache.wiki.auth.authorize.Group} object by retrieving it
     * via the Wiki{@link org.apache.wiki.auth.authorize.GroupManager}. If
     * the group name is illegal, this method will add a validation error to
     * the supplied Collection of errors. The error will be of type
     * {@link net.sourceforge.stripes.validation.LocalizableError} and will have
     * a message key of <code>group.doesnotexist</code> and a single parameter
     * (equal to the value passed for <code>groupName</code>). If the group
     * does not exist (perhaps because it is new), {@code null} is returned.
     * 
     * @param groupName the name of the WikiPage to retrieve
     * @param targetType the type to return, which will always be of type
     *            {@link org.apache.wiki.auth.authorize.Group}
     * @param errors the current Collection of validation errors for this field
     * @return the resolved Group
     */
    public Group convert( String groupName, Class<? extends Group> targetType, Collection<ValidationError> errors )
    {
        WikiRuntimeConfiguration config = (WikiRuntimeConfiguration) StripesFilter.getConfiguration();
        WikiEngine engine = config.getEngine();
        GroupManager mgr = engine.getGroupManager();
        Group group = null;
        try
        {
            group = mgr.getGroup( groupName, true );
        }
        catch( NoSuchPrincipalException e )
        {
            // Group does not exist
        }
        catch( WikiSecurityException e )
        {
            // Illegal group name
            errors.add( new LocalizableError( "editgroup.illegalname" ) );
        }
        return group;
    }

    /**
     * No-op method that does nothing, because setting the Locale has no effect on the conversion.
     */
    public void setLocale( Locale locale )
    {
    };
}
