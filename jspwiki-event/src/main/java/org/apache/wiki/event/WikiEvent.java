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

package org.apache.wiki.event;

import java.util.EventObject;

/**
 * Abstract parent class for wiki events.
 *
 * @since 2.3.79
 */
public abstract class WikiEvent extends EventObject {

    private static final long serialVersionUID = 1829433967558773960L;

    /** Indicates a exception or error state. */
    public static final int ERROR          = -99;

    /** Indicates an undefined state. */
    public static final int UNDEFINED      = -98;

    private int m_type = UNDEFINED;

    private final long m_when;

    // ............

    /**
     * Constructs an instance of this event.
     *
     * @param src the Object that is the source of the event.
     * @param type the event type.
     */
    public WikiEvent( final Object src, final int type ) {
        super( src );
        m_when = System.currentTimeMillis();
        setType( type );
    }
    
    /**
     * Convenience method that returns the typed object to which the event applied.
     * 
     * @return the typed object to which the event applied.
     */
    @SuppressWarnings("unchecked")
    public < T > T getSrc() {
        return ( T )super.getSource();
    }

   /**
    *  Returns the timestamp of when this WikiEvent occurred.
    *
    * @return this event's timestamp
    * @since 2.4.74
    */
   public long getWhen() {
       return m_when;
   }

    /**
     * Sets the type of this event. Validation of acceptable type values is the responsibility of each subclass.
     *
     * @param type the type of this WikiEvent.
     */
    protected void setType( final int type ) {
        m_type = type;
    }

    /**
     * Returns the type of this event.
     *
     * @return the type of this WikiEvent. See the enumerated values defined in {@link org.apache.wiki.event.WikiEvent}).
     */
    public int getType() {
        return m_type;
    }

    /**
     * Returns a String (human-readable) description of an event type. This should be subclassed as necessary.
     *
     * @return the String description
     */
    public String getTypeDescription() {
        switch( m_type ) {
            case ERROR:     return "exception or error event";
            case UNDEFINED: return "undefined event type";
            default:        return "unknown event type (" + m_type + ")";
        }
    }

    /**
     * Returns true if the int value is a valid WikiEvent type. Because the WikiEvent class does not itself any event types,
     * this method returns true if the event type is anything except {@link #ERROR} or {@link #UNDEFINED}. This method is meant to
     * be subclassed as appropriate.
     * 
     * @param type The value to test.
     * @return true, if the value is a valid WikiEvent type.
     */
    public static boolean isValidType( final int type ) {
        return type != ERROR && type != UNDEFINED;
    }


    /**
     * Returns a textual representation of an event type.
     *
     * @return the String representation
     */
    public String eventName() {
        switch( m_type ) {
            case ERROR:     return "ERROR";
            case UNDEFINED: return "UNDEFINED";
            default:        return "UNKNOWN (" + m_type + ")";
        }
    }

    /**
     * Prints a String (human-readable) representation of this object. This should be subclassed as necessary.
     *
     * @see java.lang.Object#toString()
     * @return the String representation
     */
    public String toString() {
        return "WikiEvent." + eventName() + " [source=" + getSource().toString() + "]";
    }

}
