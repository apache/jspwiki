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
package org.apache.wiki.tags;

import java.util.Date;
import java.text.SimpleDateFormat;
import java.io.IOException;

import org.apache.wiki.WikiPage;
import org.apache.wiki.preferences.Preferences;
import org.apache.wiki.preferences.Preferences.TimeFormat;

/**
 *  Writes the modification date of the page, formatted
 *  as specified in the attribute "format".
 *
 *  <UL>
 *   <LI>format = A string describing which format you want to use.
 *       This is exactly like in "java.text.SimpleDateFormat".
 *  </UL>
 *
 *  @since 2.0
 */

// FIXME: Should also take the current user TimeZone into account.

public class PageDateTag
    extends WikiTagBase
{
    private static final long serialVersionUID = 0L;
    
    public static final String DEFAULT_FORMAT = "dd-MMM-yyyy HH:mm:ss zzz";

    private String m_format = null;

    public void initTag()
    {
        super.initTag();
        m_format = null;
    }

    public String getFormat()
    {
        return m_format;
    }

    public void setFormat( String arg )
    {
        m_format = arg;
    }

    public final int doWikiStartTag()
        throws IOException
    {
        WikiPage   page   = m_wikiContext.getPage();

        if( page != null )
        {
            Date d = page.getLastModified();

            //
            //  Date may be null if the page does not exist.
            //
            if( d != null )
            {
                SimpleDateFormat fmt;
                
                if( m_format == null )
                    fmt = Preferences.getDateFormat( m_wikiContext, TimeFormat.DATETIME );
                else
                    fmt = new SimpleDateFormat( m_format );

                pageContext.getOut().write( fmt.format( d ) );
            }
            else
            {
                pageContext.getOut().write( "&lt;never&gt;" );
            }
        }

        return SKIP_BODY;
    }
}
