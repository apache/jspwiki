package org.apache.wiki.event;

import java.io.Serializable;

/**
 * Events fired by {@link org.apache.wiki.content.ContentManager} when nodes are
 * created, saved or deleted.
 */
public class ContentEvent extends WikiPageEvent
{
    private static final long serialVersionUID = -6577147048708900469L;

    /**
     * Indicates that a node has been requested to be deleted, but it has not
     * yet been removed from the repository.
     */
    public static final int NODE_DELETE_REQUEST = 220;

    /**
     * Indicates that a node was successfully deleted.
     */
    public static final int NODE_DELETED = 221;

    /**
     * Indicates that a node was successfully renamed.
     */
    public static final int NODE_RENAMED = 211;

    /**
     * Indicates a node was successfully saved.
     */
    public static final int NODE_SAVED = 201;

    /**
     * Constructs an instance of this event.
     * 
     * @param src the Object that is the source of the event.
     * @param type the type of the event (see the enumerated int values defined
     *            in {@link org.apache.wiki.event.WikiEvent}).
     * @param pagename the WikiPage being acted upon.
     * @param args additional arguments passed to the event.
     */
    public ContentEvent( Object src, int type, String pagename, Serializable... args )
    {
        super( src, type, pagename, args );
    }
}
