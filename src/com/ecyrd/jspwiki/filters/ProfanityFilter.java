package com.ecyrd.jspwiki.filters;

import com.ecyrd.jspwiki.WikiContext;
import com.ecyrd.jspwiki.TextUtil;

public class ProfanityFilter
    extends BasicPageFilter
{
    private static final String[] c_profanities = {
        "fuck",
        "shit" };

    public String preTranslate( WikiContext context, String content )
    {
        System.out.println("prechecking");
        for( int i = 0; i < c_profanities.length; i++ )
        {
            String word = c_profanities[i];
            String replacement = word.charAt(0)+"*"+word.charAt(word.length()-1);

            content = TextUtil.replaceString( content, word, replacement );
        }

        return content;
    }
}
