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

import java.security.Permission;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.permissions.AllPermission;
import org.apache.wiki.auth.user.UserDatabase;
import org.apache.wiki.ui.Installer;
import org.apache.wiki.ui.stripes.WikiRequestContext;

import net.sourceforge.stripes.action.*;


@UrlBinding( "/Install.jsp" )
public class InstallActionBean extends AbstractActionBean
{
    public Permission requiredPermission()
    {
        // See if admin users exists
        Permission perm = null;
        WikiEngine engine = getContext().getEngine();
        try
        {
            UserManager userMgr = engine.getUserManager();
            UserDatabase userDb = userMgr.getUserDatabase();
            userDb.findByLoginName( Installer.ADMIN_ID );
            perm = new AllPermission( engine.getApplicationName() );
        }
        catch( NoSuchPrincipalException e )
        {
            // No admin user; thus, no permission required
        }

        return perm;
    }

    @DefaultHandler
    @HandlesEvent( "install" )
    @WikiRequestContext( "install" )
    public Resolution view()
    {
        return new ForwardResolution( "/Install.jsp" );
    }
}
