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

import java.util.List;

import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.HandlerPermission;
import org.apache.wiki.ui.stripes.WikiRequestContext;

import net.sourceforge.stripes.action.FileBean;
import net.sourceforge.stripes.action.HandlesEvent;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.action.UrlBinding;

@UrlBinding( "/Upload.jsp" )
public class UploadActionBean extends AbstractActionBean
{
    private Logger log = LoggerFactory.getLogger( UploadActionBean.class );

    private List<FileBean> m_newAttachments;

    /**
     * Sets the set of new attachments that should be saved when the
     * {@link #upload()} event is executed.
     * 
     * @param newAttachments the new files to attach
     */
    public void setNewAttachments( List<FileBean> newAttachments )
    {
        m_newAttachments = newAttachments;
    }

    /**
     * Handler method that uploads a new attachment to the ViewActionBean.
     * 
     * @return
     */
    @HandlesEvent( "upload" )
    @HandlerPermission( permissionClass = PagePermission.class, target = "${page.qualifiedName}", actions = PagePermission.VIEW_ACTION )
    @WikiRequestContext( "upload" )
    public Resolution upload()
    {
        log.debug( "Executed upload; " + m_newAttachments.size() + " attachments found." );
        return null;
    }

}
