/* 
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
package org.apache.wiki.ui.admin.beans;

import java.util.Date;

import javax.management.NotCompliantMBeanException;
import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.WikiSession;
import org.apache.wiki.auth.NoSuchPrincipalException;
import org.apache.wiki.auth.UserManager;
import org.apache.wiki.auth.WikiSecurityException;
import org.apache.wiki.auth.user.UserProfile;
import org.apache.wiki.ui.admin.AdminBean;
import org.apache.wiki.ui.admin.SimpleAdminBean;


public class UserBean extends SimpleAdminBean
{
    public UserBean( WikiEngine engine ) throws NotCompliantMBeanException
    {
        super();
    }

    public String[] getAttributeNames()
    {
        return new String[0];
    }

    // FIXME: We don't yet support MBean for this kind of stuff.
    public String[] getMethodNames()
    {
        return new String[0];
    }



    public String doPost(WikiContext context)
    {
        HttpServletRequest request = context.getHttpRequest();
        WikiSession session = context.getWikiSession();
        UserManager mgr = context.getEngine().getUserManager();

        String loginid   = request.getParameter("loginid");
        String loginname = request.getParameter("loginname");
        String fullname  = request.getParameter("fullname");
        String password  = request.getParameter("password");
        String password2 = request.getParameter("password2");
        String email     = request.getParameter("email");


        if( request.getParameter("action").equalsIgnoreCase("remove") )
        {
            try
            {
                mgr.getUserDatabase().deleteByLoginName(loginid);
                session.addMessage("User profile "+loginid+" ("+fullname+") has been deleted");
            }
            catch (NoSuchPrincipalException e)
            {
                session.addMessage("User profile has already been removed");
            }
            catch (WikiSecurityException e)
            {
                session.addMessage("Security problem: "+e);
            }
            return "";
        }


        if( password != null && password.length() > 0 && !password.equals(password2) )
        {
            session.addMessage("Passwords do not match!");
            return "";
        }

        UserProfile p;

        if( loginid.equals("--New--") )
        {
            // Create new user

            p = mgr.getUserDatabase().newProfile();
            p.setCreated( new Date() );
        }
        else
        {
            try
            {
                p = mgr.getUserDatabase().findByLoginName( loginid );
            }
            catch (NoSuchPrincipalException e)
            {
                session.addMessage("I could not find user profile "+loginid);
                return "";
            }
        }

        p.setEmail(email);
        p.setFullname(fullname);
        if( password != null && password.length() > 0 ) p.setPassword(password);
        p.setLoginName(loginname);

        try
        {
            mgr.getUserDatabase().save( p );
        }
        catch( WikiSecurityException e )
        {
            session.addMessage("Unable to save "+e.getMessage());
        }

        session.addMessage("User profile has been updated");

        return "";
    }

    public String getTitle()
    {
        return "User administration";
    }

    public int getType()
    {
        return AdminBean.UNKNOWN;
    }

}
