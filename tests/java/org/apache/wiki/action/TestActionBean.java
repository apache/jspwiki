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

package org.apache.wiki.action;

import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.validation.Validate;

import org.apache.wiki.api.WikiPage;
import org.apache.wiki.ui.stripes.SpamProtect;

/**
 * Simple test bean with a single event handler method.
 */
public class TestActionBean extends AbstractPageActionBean
{
    @SpamProtect( content = "text" )
    @HandlesEvent( "test" )
    public Resolution test()
    {
        return null;
    }
    
    @Validate( required = false )
    public void setPage( WikiPage page )
    {
        super.setPage( page );
    }
    
    private String text = null;
    
    private String acl = null;

    public String getText()
    {
        return text;
    }

    public void setText( String text )
    {
        this.text = text;
    }

    public String getAcl()
    {
        return acl;
    }

    public void setAcl( String acl )
    {
        this.acl = acl;
    }
    
}