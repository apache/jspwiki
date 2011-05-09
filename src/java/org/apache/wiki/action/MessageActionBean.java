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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;

import net.sourceforge.stripes.action.*;

import org.apache.wiki.WikiContext;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.TemplateResolution;
import org.apache.wiki.ui.stripes.WikiRequestContext;
import org.apache.wiki.util.FileUtil;

@UrlBinding( "/Message.jsp" )
public class MessageActionBean extends AbstractActionBean
{
    private static final Logger LOG = LoggerFactory.getLogger( "JSPWiki" );

    private String m_message = null;

    private Throwable m_realcause = null;

    /**
     * Event that forwards control to the template JSP {@code Error.jsp}.
     * It also traps the cause of the exception and logs the details.
     * 
     * @return the forward resolution
     */
    @DontValidate
    @HandlesEvent( "error" )
    @WikiRequestContext( "error" )
    public Resolution error()
    {
        WikiContext wikiContext = getContext();
        HttpServletRequest request = wikiContext.getHttpRequest();
        String msg = "JSPWiki encountered an error.";
        Exception exception = (Exception) request.getAttribute( PageContext.EXCEPTION );

        if( exception != null )
        {
            msg = exception.getMessage();
            if( msg == null || msg.length() == 0 )
            {
                msg = "JSPWiki encountered a " + exception.getClass().getName() + " exception.";
            }
            setMessage( msg );

            // Get the actual cause of the exception.
            // Note the cast; at least Tomcat has two classes called
            // "JspException"
            // imported in JSP pages.

            if( exception instanceof javax.servlet.jsp.JspException )
            {
                LOG.debug( "IS JSPEXCEPTION" );
                m_realcause = ((javax.servlet.jsp.JspException) exception).getRootCause();
                LOG.debug( "REALCAUSE=" + m_realcause );
            }

            if( m_realcause == null )
                m_realcause = exception;
        }
        else
        {
            m_realcause = new Exception( "Unknown cause." );
        }

        LOG.debug( "Error.jsp exception is: ", exception );
        return new TemplateResolution( "Error.jsp" );
    }

    /**
     * Returns the message string for this MessageActionBean. When the {@code
     * message} event executes, the message will be the value of request
     * parameter {@code message}. When the {@code error} event method executes,
     * this method returns the appropriate Exception message.
     * 
     * @return the message
     */
    public String getMessage()
    {
        return m_message;
    }

    /**
     * If the {@code error} event method is executed, returns the Throwable that
     * caused the error.
     * 
     * @return the Exception, or {@code null}
     */
    public Throwable getRealCause()
    {
        return m_realcause;
    }
    
    /**
     * If the {@code error} event method is executed, returns the class in which the
     * exception occured.
     * 
     * @return the {@link Class}, or {@code null}
     */
    public Class<? extends Throwable> getRealCauseClass()
    {
        return m_realcause.getClass();
    }

    /**
     * If the {@code error} event method is executed, returns the Class, Method
     * and line number that caused the error.
     * 
     * @return the Exception, or {@code null}
     */
    public String getThrowingMethod()
    {
        return m_realcause == null ? null : FileUtil.getThrowingMethod( m_realcause );
    }

    /**
     * Default event that forwards control to /Message.jsp.
     * 
     * @return the forward resolution
     */
    @DefaultHandler
    @DontValidate
    @HandlesEvent( "message" )
    @WikiRequestContext( "message" )
    public Resolution message()
    {
        return new TemplateResolution( "Message.jsp" );
    }

    /**
     * Sets the message for this MessageActionBean. If {@code message} is not
     * {@code null}, this method also puts the value into request scope under
     * the attribute name {@code message}.
     * 
     * @param message
     */
    public void setMessage( String message )
    {
        m_message = message;
        if( m_message != null )
        {
            HttpServletRequest request = getContext().getRequest();
            request.setAttribute( "message", m_message );
        }
    }
}
