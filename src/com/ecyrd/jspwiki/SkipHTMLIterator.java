
package com.ecyrd.jspwiki;

import java.text.*;


public class SkipHTMLIterator
    extends BreakIterator
{
    String m_text;
    int    m_loc = 0;

    public SkipHTMLIterator()
    {
    }

    public int current()
    {
        return m_loc;
    }

    public int first()
    {
        // Eat away whitespace and non-letters
        for( int i = 0; i < m_text.length(); i++ )
        {
            if( m_text.charAt(i) == '<' )
            {
                while( m_text.charAt(i++) != '>' && i < m_text.length() );
            }

            if( Character.isLetterOrDigit( m_text.charAt(i) ) )
            {
                m_loc = i;
                break;
            }
        }

        return m_loc;
    }

    public int last()
    {
        return m_text.length();
    }

    public int next()
    {
        int res = following( m_loc );

        m_loc = res;

        return m_loc;
    }

    public int next( int n )
    {
        int res = 0;

        for( int i = 0; i < n; i++ )
        {
            res = next();
        }

        return res;
    }

    public int following( int offset )
    {

        // Eat away whitespace and non-letters
        for( int i = offset; i < m_text.length(); i++ )
        {
            if( m_text.charAt(i) == '<' )
            {
                while( m_text.charAt(i++) != '>' && i < m_text.length() );
            }

            if( Character.isLetterOrDigit( m_text.charAt(i) ) )
            {
                offset = i;
                break;
            }
        }

        // Find end of this word (aka the first whitespace)
        for( int i = offset+1; i < m_text.length(); i++ )
        {
            if( m_text.charAt(i) == '<' )
            {
                while( m_text.charAt(i++) != '>' && i < m_text.length() );
            }

            if( !Character.isLetterOrDigit( m_text.charAt(i) ) )
            {
                return i;
            }
        }

        return DONE;
    }


    public int previous()
    {
        return 0;
    }

    public CharacterIterator getText()
    {
        return new StringCharacterIterator(m_text);
    }

    public void setText( CharacterIterator ci )
    {
    }

    public void setText( String text )
    {
        m_text = text;
        m_loc = 0;
    }
}
