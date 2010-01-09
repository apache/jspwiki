/**
 * 
 */
package org.apache.wiki.content.inspect;

import org.apache.commons.jrcs.diff.*;
import org.apache.commons.jrcs.diff.myers.MyersDiff;
import org.apache.wiki.WikiContext;
import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiPage;
import org.apache.wiki.providers.ProviderException;

/**
 * Embodies the differences between two Strings, or between a proposed text
 * and current value for a field. This class is immutable and therefore
 * thread-safe.
 */
public class Change
{
    private final String m_name;

    private final String m_value;

    private final String m_change;

    private final int m_adds;

    private final int m_removals;

    /**
     * Convenience method that returns a Change object representing a new text
     * string. String {@code newText} will be represented as a single add.
     * 
     * @param name the name of the field that is being changed
     * @param newText the new text
     * @return the Change object
     */
    public static Change getChange( String name, String newText )
    {
        return new Change( name, newText, 1, 0, newText );
    }

    /**
     * Compares a proposed (new) text string against an existing (old) String,
     * and returns a Change object representing any differences between the two.
     * 
     * @param name the name of the field that is being changed
     * @param oldText the old text
     * @param newText the new text
     * @return the change, or the empty string if there is no difference
     */
    public static Change getChange( String name, String oldText, String newText ) throws DifferentiationFailedException
    {
        if( oldText == null || newText == null )
        {
            throw new IllegalArgumentException( "Parameters oldText and newText must be supplied." );
        }

        StringBuffer changes = new StringBuffer();
        String[] first = Diff.stringToArray( oldText );
        String[] second = Diff.stringToArray( newText );
        Revision rev = Diff.diff( first, second, new MyersDiff() );

        if( rev == null || rev.size() == 0 )
        {
            return new Change( name, newText, 0, 0, null );
        }

        int adds = 0;
        int removals = 0;
        for( int i = 0; i < rev.size(); i++ )
        {
            Delta d = rev.getDelta( i );
            String suffix = (i < rev.size() - 1) ? "\r\n" : "";

            if( d instanceof AddDelta )
            {
                d.getRevised().toString( changes, "", suffix );
                adds++;
            }
            else if( d instanceof ChangeDelta )
            {
                d.getRevised().toString( changes, "", suffix );
                adds++;
            }
            else if( d instanceof DeleteDelta )
            {
                removals++;
            }
        }
        return new Change( name, newText, adds, removals, changes.toString() );
    }

    public String getName()
    {
        return m_name;
    }

    /**
     * Compares a proposed text String for a page against the page's current
     * content, and returns a Change object representing any differences between
     * the two. The field's name is {@code page}.
     * 
     * @param context the wiki context containing the page. This must be a
     *            page-related WikiContext
     * @param newText the new text
     * @return Empty string, if there is no change.
     * @throws DifferentiationFailedException if the page contents could not be retrieved or diffed
     */
    public static Change getPageChange( WikiContext context, String newText ) throws DifferentiationFailedException
    {
        WikiPage page = context.getPage();
        WikiEngine engine = context.getEngine();
        StringBuilder changes = new StringBuilder();

        // Get current page version
        if( engine.pageExists( page.getName() ) )
        {
            String oldText;
            try
            {
                oldText = page.getContentAsString();
            }
            catch( ProviderException e )
            {
                e.printStackTrace();
                throw new DifferentiationFailedException( "Differentiation failed: " + e.getMessage() );
            }
            changes.append( oldText );

            // Don't forget to include the change note, too
            String changeNote = (String) page.getAttribute( WikiPage.CHANGENOTE );
            if( changeNote != null )
            {
                changes.append( "\r\n" );
                changes.append( changeNote );
            }

            // And author as well
            if( page.getAuthor() != null )
            {
                changes.append( "\r\n" + page.getAuthor() );
            }
        }

        return getChange( "page", changes.toString(), newText );
    }

    private Change( String name, String newText, int adds, int removals, String change )
    {
        super();
        m_name = name;
        m_value = newText;
        m_adds = adds;
        m_removals = removals;
        m_change = change;
    }

    public boolean equals( Object o )
    {
        if( o instanceof Change )
            return m_name.equals( ((Change) o).m_name ) && m_change.equals( ((Change) o).m_change );

        return false;
    }

    public int getAdds()
    {
        return m_adds;
    }

    public String getChange()
    {
        return m_change;
    }

    public int getRemovals()
    {
        return m_removals;
    }

    /**
     * Returns the current (i.e., new) value of the Change. 
     * @return the current value
     */
    public String getValue()
    {
        return m_value;
    }

    public int hashCode()
    {
        return m_change.hashCode() + 17;
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return m_change;
    }
}
