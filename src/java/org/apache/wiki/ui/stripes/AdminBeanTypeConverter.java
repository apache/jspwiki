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

import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.TypeConverter;
import net.sourceforge.stripes.validation.ValidationError;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.ui.admin.AdminBean;
import org.apache.wiki.ui.admin.AdminBeanManager;

/**
 * Stripes type converter that converts an {@link AdminBean} ID, expressed as a
 * String, into an AdminBean object. This converter is looked up and returned by
 * the Stripes {@link net.sourceforge.stripes.validation.TypeConverterFactory}
 * for HTTP request parameters that need to be bound to ActionBean properties of
 * type AdminBean. Stripes executes this TypeConverter during the
 * {@link net.sourceforge.stripes.controller.LifecycleStage#BindingAndValidation}
 * stage of request processing.
 */
public class AdminBeanTypeConverter implements TypeConverter<AdminBean>
{
    /**
     * Converts a named AdminBean into a valid {@link AdminBean} object by
     * retrieving it via the Wiki
     * {@link org.apache.wiki.ui.admin.AdminBeanManager}. If the bean ID is
     * illegal, this method will add a validation error to the supplied
     * Collection of errors. The error will be of type
     * {@link net.sourceforge.stripes.validation.LocalizableError} and will have
     * a message key of <code>AdminBeanTypeConverter.illegalAdminBean</code> and
     * a single parameter {2} (equal to the value passed for <code>beanId</code>
     * ). If the bean ID does not exist, {@code null} is returned also.
     * 
     * @param beanId the AdminBean ID to look up
     * @param targetType the type to return, which will always be of type
     *            {@link AdminBean}
     * @param errors the current Collection of validation errors for this field
     * @return the resolved AdminBean
     */
    public AdminBean convert( String beanId, Class<? extends AdminBean> targetType, Collection<ValidationError> errors )
    {
        WikiRuntimeConfiguration config = (WikiRuntimeConfiguration) StripesFilter.getConfiguration();
        WikiEngine engine = config.getEngine();
        AdminBeanManager mgr = engine.getAdminBeanManager();
        AdminBean bean = mgr.findBean( beanId );

        // AdminBean does not exist
        if( bean == null )
        {
            // Illegal group name
            errors.add( new LocalizableError( "AdminBeanTypeConverter.illegalAdminBean", beanId ) );
        }
        return bean;
    }

    /**
     * No-op method that does nothing, because setting the Locale has no effect
     * on the conversion.
     */
    public void setLocale( Locale locale )
    {
    };
}
