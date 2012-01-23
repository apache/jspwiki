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

package org.apache.wiki.event;

import org.apache.wiki.WikiEngine;

/**
 *  A utility class that adds some JSPWiki-specific functionality to the
 *  WikiEventManager (which is really a general-purpose event manager).
 *
 * @since 2.4.20
 */
public class WikiEventUtils
{
    /**
     *  This ungainly convenience method adds a WikiEventListener to the
     *  appropriate component of the provided client Object, to listen
     *  for events of the provided type (or related types, see the table
     *  below).
     *  <p>
     *  If the type value is valid but does not match any WikiEvent type
     *  known to this method, this will just attach the listener to the
     *  client Object. This may mean that the Object never fires events
     *  of the desired type; type-to-client matching is left to you to
     *  guarantee. Silence is golden, but not if you want those events.
     *  </p>
     *  <p>
     *  Most event types expect a WikiEngine as the client, with the rest
     *  attaching the listener directly to the supplied source object, as
     *  described below:
     *  </p>
     *  <table border="1" cellpadding="4">
     *    <tr><th>WikiEvent Type(s) </th><th>Required Source Object </th><th>Actually Attached To </th>
     *    </tr>
     *    <tr><td>any WikiEngineEvent       </td><td>WikiEngine  </td><td>WikiEngine        </td></tr>
     *    <tr><td>WikiPageEvent.PAGE_LOCK,
     *            WikiPageEvent.PAGE_UNLOCK </td><td>WikiEngine or
     *                                               PageManager </td><td>PageManager       </td></tr>
     *    <tr><td>WikiPageEvent.PAGE_REQUESTED,
     *            WikiPageEvent.PAGE_DELIVERED </td>
     *                                           <td>WikiServletFilter </td>
     *                                                                <td>WikiServletFilter </td></tr>
     *    <tr><td>WikiPageEvent (<a href="#pbeTypes">phase boundary event</a>)</td>
     *                                           <td>WikiEngine  </td><td>FilterManager     </td></tr>
     *    <tr><td>WikiPageEvent (<a href="#ipeTypes">in-phase event</a>)</td>
     *    <tr><td>WikiPageEvent (in-phase event)</td>
     *                                           <td>any         </td><td>source object     </td></tr>
     *    <tr><td>WikiSecurityEvent         </td><td>any         </td><td>source object     </td></tr>
     *    <tr><td>any other valid type      </td><td>any         </td><td>source object     </td></tr>
     *    <tr><td>any invalid type          </td><td>any         </td><td>nothing           </td></tr>
     *  </table>
     *
     * <p id="pbeTypes"><small><b>phase boundary event types:</b>
     * <tt>WikiPageEvent.PRE_TRANSLATE_BEGIN</tt>, <tt>WikiPageEvent.PRE_TRANSLATE_END</tt>,
     * <tt>WikiPageEvent.POST_TRANSLATE_BEGIN</tt>, <tt>WikiPageEvent.POST_TRANSLATE_END</tt>,
     * <tt>WikiPageEvent.PRE_SAVE_BEGIN</tt>, <tt>WikiPageEvent.PRE_SAVE_END</tt>,
     * <tt>WikiPageEvent.POST_SAVE_BEGIN</tt>, and <tt>WikiPageEvent.POST_SAVE_END</tt>.
     * </small></p>
     * <p id="ipeTypes"><small><b>in-phase event types:</b>
     * <tt>WikiPageEvent.PRE_TRANSLATE</tt>, <tt>WikiPageEvent.POST_TRANSLATE</tt>,
     * <tt>WikiPageEvent.PRE_SAVE</tt>, and <tt>WikiPageEvent.POST_SAVE</tt>.
     * </small></p>
     *
     * <p>
     * <b>Note:</b> The <i>Actually Attached To</i> column may also be considered as the
     * class(es) that fire events of the type(s) shown in the <i>WikiEvent Type</i> column.
     * </p>
     *
     * @see org.apache.wiki.event.WikiEvent
     * @see org.apache.wiki.event.WikiEngineEvent
     * @see org.apache.wiki.event.WikiPageEvent
     * @see org.apache.wiki.event.WikiSecurityEvent
     * @throws ClassCastException if there is a type mismatch between certain event types and the client Object
     */
    public static synchronized void addWikiEventListener(
            Object client, int type, WikiEventListener listener )
    {
        // Make sure WikiEventManager exists
        WikiEventManager.getInstance();
        
        // FIXME: Seriously, what is all this crap code below? We've made it impossible to use the
        // same event type with more than one source. Isn't the listener pattern supposed to
        // stop stuff like this? Bueller?
        
        // first, figure out what kind of event is expected to be generated this does
        // tie us into known types, but WikiEvent.isValidType() will return true so
        // long as the type was set to any non-ERROR or non-UNKNOWN value

        if ( WikiEngineEvent.isValidType(type) ) // add listener directly to WikiEngine
        {
            WikiEventManager.addWikiEventListener( client, listener );
        }
        else if ( WikiPageEvent.isValidType(type) ) // add listener to one of several options
        {
            if(  type == WikiPageEvent.PAGE_LOCK
              || type == WikiPageEvent.PAGE_UNLOCK ) // attach to PageManager
            {
                if( client instanceof WikiEngine )
                {
                    WikiEventManager.addWikiEventListener( ((WikiEngine)client).getContentManager(), listener );
                }
                else // if ( client instanceof PageManager ) // no filter?
                {
                    WikiEventManager.addWikiEventListener( client, listener );
                }
            }
            else if(  type == WikiPageEvent.PAGE_REQUESTED
                   || type == WikiPageEvent.PAGE_DELIVERED ) // attach directly to WikiServletFilter
            {
                WikiEventManager.addWikiEventListener( client, listener );
            }
            else if(  type == WikiPageEvent.PRE_TRANSLATE_BEGIN
                   || type == WikiPageEvent.PRE_TRANSLATE_END
                   || type == WikiPageEvent.POST_TRANSLATE_BEGIN
                   || type == WikiPageEvent.POST_TRANSLATE_END
                   || type == WikiPageEvent.PRE_SAVE_BEGIN
                   || type == WikiPageEvent.PRE_SAVE_END
                   || type == WikiPageEvent.POST_SAVE_BEGIN
                   || type == WikiPageEvent.POST_SAVE_END ) // attach to FilterManager
            {
                WikiEventManager.addWikiEventListener( ((WikiEngine)client).getFilterManager(), listener );
            }
            else //if (  type == WikiPageEvent.PRE_TRANSLATE
                 // || type == WikiPageEvent.POST_TRANSLATE
                 // || type == WikiPageEvent.PRE_SAVE
                 // || type == WikiPageEvent.POST_SAVE ) // attach to client
            {
                WikiEventManager.addWikiEventListener( client, listener );
            }
        }
        else if( WikiSecurityEvent.isValidType(type) ) // add listener to the client
        {
            // currently just attach it to the client (we are ignorant of other options)
            WikiEventManager.addWikiEventListener( client, listener );
        }
        else if( WikiEvent.isValidType(type) ) // we don't know what to do
        {
            WikiEventManager.addWikiEventListener( client, listener );
        }
        else // is error or unknown
        {
            // why are we being called with this?
        }
    }

} // end org.apache.wiki.event.WikiEventUtils
