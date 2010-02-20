package org.apache.wiki.ui.stripes;

import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.TimeZone;

import net.sourceforge.stripes.format.Formatter;

import org.apache.wiki.i18n.InternationalizationManager;

/**
 * Stripes Formatter class that formats a supplied wiki
 * {@link java.util.TimeZone}. Two formatting types are available: {@code
 * localized} produces a long-form, localized label (e.g., {@code [GMT-5]
 * Eastern Standard Time}); format type {@code id} generates the time zone ID
 * (e.g., {@code America/New_York}). If an unsupported format type is supplied,
 * the value will default to {@link TimeZone#toString()}.
 */
public class TimeZoneFormatter implements Formatter<TimeZone>
{
    private Locale m_locale = null;

    private String m_formatType = null;

    /**
     * The default format type that produces a localized time zone string.
     */
    public static final String LOCALIZED = "localized";

    /**
     * An alternative format type that produces the time zone ID.
     */
    public static final String ID = "id";

    /** I18N string to mark the server timezone */
    public static final String I18NSERVER_TIMEZONE = "prefs.user.timezone.server";

    /**
     * {@inheritDoc}
     * <p>
     * This implementation produces a formatted String representing the
     * {@link TimeZone}.
     * </p>
     */
    public String format( TimeZone zone )
    {
        if ( m_formatType == null ) m_formatType = LOCALIZED;
        if( LOCALIZED.equals( m_formatType ) )
        {
            java.util.TimeZone serverZone = java.util.TimeZone.getDefault();
            Date now = new Date();

            int offset = zone.getRawOffset() / 3600000;
            String zoneLabel = "[GMT" + (offset > 0 ? "+" : "") + offset + "] "
                               + zone.getDisplayName( zone.inDaylightTime( now ), TimeZone.LONG, m_locale );
            if( serverZone.getRawOffset() == zone.getRawOffset() )
            {
                ResourceBundle b = ResourceBundle.getBundle( InternationalizationManager.TEMPLATES_BUNDLE, m_locale );
                String serverLabel = b.getString( I18NSERVER_TIMEZONE );
                zoneLabel = zoneLabel + " " + serverLabel;
            }
            return zoneLabel;
        }
        else if( ID.equals( m_formatType ) )
        {
            return zone.getID();
        }
        return zone.toString();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does nothing.
     * </p>
     */
    public void init()
    {
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation does nothing; the time zone name is always used.
     * </p>
     */
    public void setFormatPattern( String formatPattern )
    {
    }

    /**
     * <p>
     * Sets the format type. The default is {@link #LOCALIZED}
     * </p>
     */
    public void setFormatType( String formatType )
    {
        m_formatType = formatType;
    }

    /**
     * {@inheritDoc}
     */
    public void setLocale( Locale locale )
    {
        m_locale = locale;
    }

}
