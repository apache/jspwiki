package com.ecyrd.jspwiki;

public class QueryItem
{
    public static final int REQUIRED  = 1;
    public static final int FORBIDDEN = -1;
    public static final int REQUESTED = 0;

    public String word;
    public int    type = REQUESTED;
}
