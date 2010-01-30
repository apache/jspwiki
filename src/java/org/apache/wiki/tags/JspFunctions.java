package org.apache.wiki.tags;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.wiki.api.WikiPage;
import org.apache.wiki.content.jcr.JCRWikiPage;
import org.apache.wiki.i18n.InternationalizationManager;

/**
 * JSPWiki functions used in JSP 2.0 tags.
 */
public class JspFunctions
{
    /**
     * <p>
     * Returns a localized attachment tab name based on the number of
     * attachments. The length of the collection will be passed to a message
     * looked up in {@link InternationalizationManager#CORE_BUNDLE}. If the
     * length is 0, the message key will be {@code attachments.none}. If the
     * length is 1, the key will be {@code attachments.one}. Otherwise, the key
     * will be {@code attachments.many}. After the key is determined, the
     * message will be looked up and the localized message will be returned.
     * </p>
     * <p>
     * For example, for the Locale of English, attachment collection sizes of 0,
     * 1 and 2 might produce {@code Attachments}, {@code Attachment (1)} and
     * {@code Attachments (2)}.
     * </p>
     * 
     * @param locale the user's locale
     * @param attachments the number of attachments
     * @return the localized message
     */
    public static String getAttachmentsTitle( Locale locale, Collection<WikiPage> attachments )
    {
        if( locale == null )
        {
            locale = Locale.getDefault();
        }
        try
        {
            ResourceBundle b = ResourceBundle.getBundle( InternationalizationManager.TEMPLATES_BUNDLE, locale );
            String key = null;
            switch( attachments.size() )
            {
                case 0: {
                    key = "attachments.zero";
                    break;
                }
                case 1: {
                    key = "attachments.one";
                    break;
                }
                default: {
                    key = "attachments.many";
                }
            }
            String message = b.getString( key );
            return MessageFormat.format( message, attachments.size() );
        }
        catch( MissingResourceException e )
        {
        }
        return "???CORE_BUNDLE missing???";
    }

    /**
     * Returns a Date formatted in ISO8601 format. Ideal for sorting.
     * 
     * @param date the date to format
     * @return the formatted date, or the empty string if {@code date} is
     *         {@code null}
     */
    public static String getISO8601Date( Date date )
    {
        if( date == null )
        {
            return "";
        }
        return new SimpleDateFormat( JCRWikiPage.DATEFORMAT_ISO8601_2000 ).format( date );
    }

    /**
     * Returns a String whose characters above a supplied length are replaced by
     * the ellipsis.
     * 
     * @param string the String to shorten
     * @param length the length above which the String should be shortened
     * @return the shortened string, or the empty string if {@code string} is
     *         {@code null}
     */
    public static String shortenedString( String string, int length )
    {
        if ( string == null )
        {
            return "";
        }
        return string.length() <= length ? string :string.substring( 0, length ) + "...";
    }

}
