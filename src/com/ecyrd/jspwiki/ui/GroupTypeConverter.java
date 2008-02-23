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
package com.ecyrd.jspwiki.ui;

import java.util.Collection;
import java.util.Locale;

import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;

import com.ecyrd.jspwiki.WikiEngine;
import com.ecyrd.jspwiki.auth.NoSuchPrincipalException;
import com.ecyrd.jspwiki.auth.authorize.Group;
import com.ecyrd.jspwiki.auth.authorize.GroupManager;

/**
 * Stripes type converter that converts a Group name, expressed as a String,
 * into an {@link com.ecyrd.jspwiki.auth.authorize.Group} object. This converter
 * is looked up and returned by {@link WikiTypeConverterFactory} for HTTP
 * request parameters that need to be bound to ActionBean properties of type
 * Group. Stripes executes this TypeConverter during the
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
 * stage of request processing.
 * 
 * @author Andrew Jaquith
 */
public class GroupTypeConverter implements TypeConverter<Group>
{
    /**
     * Converts a named wiki group into a valid
     * {@link com.ecyrd.jspwiki.auth.authorize.Group} object by retrieving it
     * via the Wiki{@link com.ecyrd.jspwiki.auth.authorize.GroupManager}. If
     * the group cannot be found (perhaps because it does not exist), this
     * method will add a validation error to the supplied Collection of errors.
     * The error will be of type
     * {@link net.sourceforge.stripes.validation.LocalizableError} and will have
     * a message key of <code>pageNotFound</code> and a single parameter
     * (equal to the value passed for <code>groupName</code>).
     * 
     * @param groupName
     *            the name of the WikiPage to retrieve
     * @param targetType
     *            the type to return, which will always be of type
     *            {@link com.ecyrd.jspwiki.auth.authorize.Group}
     * @param errors
     *            the current Collection of validation errors for this field
     * @return the resolved Group
     */
    public Group convert(String groupName, Class<? extends Group> targetType, Collection<ValidationError> errors)
    {
        WikiRuntimeConfiguration config = (WikiRuntimeConfiguration)StripesFilter.getConfiguration();
        WikiEngine engine = config.getEngine();
        GroupManager mgr = engine.getGroupManager();
        Group group = null;
        try
        {
            group = mgr.getGroup(groupName);
        }
        catch (NoSuchPrincipalException e)
        {
            errors.add(new LocalizableError("groupNotFound", groupName));
        }
        return group;
    }

    public void setLocale(Locale locale)
    {
    };
}
