package com.ecyrd.jspwiki;

public interface SearchResult
{
    /**
     *  Return the name of the page.
     */
    public String getName();

    /**
     *  Returns the score.
     */

    public int getScore();
}
