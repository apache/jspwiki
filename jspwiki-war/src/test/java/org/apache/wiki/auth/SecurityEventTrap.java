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
package org.apache.wiki.auth;

import java.util.ArrayList;
import java.util.List;

import org.apache.wiki.event.WikiEvent;
import org.apache.wiki.event.WikiEventListener;
import org.apache.wiki.event.WikiSecurityEvent;

/**
 * Traps the most recent WikiEvent so that it can be used in assertions.
 * @since 2.3.79
 */
public class SecurityEventTrap implements WikiEventListener
{
    private WikiSecurityEvent m_lastEvent = null;
    private List<WikiSecurityEvent> m_events    = new ArrayList<WikiSecurityEvent>();

    public void actionPerformed( WikiEvent event )
    {
        if ( event instanceof WikiSecurityEvent )
        {
            m_lastEvent = (WikiSecurityEvent)event;
            m_events.add( (WikiSecurityEvent)event );
        }
        else
        {
            throw new IllegalArgumentException( "Event wasn't a WikiSecurityEvent. Check the unit test code!" );
        }
    }
    
    public WikiSecurityEvent lastEvent()
    {
        return m_lastEvent;
    }
    
    public void clearEvents()
    {
        m_events.clear();
    }
    
    public WikiSecurityEvent[] events() 
    {
        return m_events.toArray(new WikiSecurityEvent[m_events.size()]);
    }

}
