package org.apache.wiki.content.inspect;

import java.io.IOException;
import java.util.Properties;

import net.sourceforge.stripes.util.CryptoUtil;

import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.SpamInterceptor;
import org.apache.wiki.ui.stripes.SpamProtect;

/**
 * <p>
 * Generic {@link Inspector} interface that provides generalized CAPTCHA
 * capabilities to the content-inspection system. The CaptchaInspector consults
 * a {@link Captcha} class, which implements the necessary scripting and
 * processing logic needed to test the CAPTCHA.
 * </p>
 * It is invoked as part of the content-inspection chain called by
 * {@link SpamInterceptor}, which itself is triggered whenever a Stripes event
 * method with a {@link SpamProtect} annotation is called. In the same manner as
 * other {@link Inspector} implementations, the CaptchaInspector is expected to
 * implement the {@link #inspect(Inspection, String, Change)} method to signal
 * whether a CAPTCHA test passed or not. Failed tests should return (for
 * example) {@link Finding.Result#FAILED}.</li> </ol>
 */
public class CaptchaInspector implements Inspector
{
    public static final String CAPTCHA_NEEDED_PARAM = "_cn";

    protected static final String PROP_CAPTCHA_CLASS = "jspwiki.captcha.implementation";

    private static final Logger log = LoggerFactory.getLogger( CaptchaInspector.class );

    private Captcha m_captcha = null;

    /**
     * Initializes the CAPTCHA inspector by creating a new instance of the
     * {@link Captcha} implementation specified in the wiki property
     * {@link #PROP_CAPTCHA_CLASS}. If no CAPTCHA class is specified or if the
     * implementation cannot be located, tests will not be run.
     */
    public void initialize( InspectionPlan config )
    {
        // Get the CAPTCHA inspector, if one was specified
        m_captcha = initCaptcha( config.getProperties() );
    }

    /**
     * Tests the CAPTCHA by calling {@link Captcha#check(Inspection)}. If the
     * test passes, the result will have no effect on the Inspection. If it
     * fails, a new Finding of type {@link Finding.Result#FAILED} will be
     * created.
     */
    public Finding[] inspect( Inspection inspection, String content, Change change )
    {
        if( m_captcha == null )
        {
            return new Finding[] { new Finding( Topic.SPAM, Finding.Result.NO_EFFECT, "No CAPTCHA configured." ) };
        }

        // Was a CAPTCHA required?
        boolean captchaRequired = true;
        String encryptedCaptchaParam = inspection.getContext().getHttpParameter( CAPTCHA_NEEDED_PARAM );
        if( encryptedCaptchaParam != null )
        {
            String captchaParam = CryptoUtil.decrypt( encryptedCaptchaParam );
            if( captchaParam != null )
            {
                captchaRequired = !"false".equals( captchaParam );
            }
        }

        // If required (default is YES), go check it
        if( captchaRequired )
        {
            try
            {
                if ( m_captcha.check( inspection ) );
                {
                    return new Finding[] { new Finding( Topic.SPAM, Finding.Result.NO_EFFECT, "CAPTCHA test succeeded." ) };
                }
            }
            catch( IOException e )
            {
                return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "CAPTCHA error: " + e.getMessage() ) };
            }
        }
        return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED, "CAPTCHA test failed." ) };
    }

    /**
     * Package-private method that returns the {@link Captcha}
     * if one was successfully initialized.
     * @return the Captcha, or {@code null} if one was not specified
     * in the properties file (or not configured)
     */
    Captcha getCaptcha()
    {
        return m_captcha;
    }
    
    /**
     * Returns an new instance of the configured CAPTCHA implementation class,
     * if one was specified by the {@link #PROP_CAPTCHA_CLASS} property.
     * 
     * @param props the properties to examine
     * @return the CAPTCHA implementing class, or null if not specified or not
     *         found
     */
    protected static Captcha initCaptcha( Properties props )
    {
        String captchaClassName = props.getProperty( PROP_CAPTCHA_CLASS );
        Class<?> captchaClass = null;
        if( captchaClassName != null )
        {
            try
            {
                captchaClass = Class.forName( captchaClassName );
            }
            catch( ClassNotFoundException e )
            {
                // Not found! Better log this.
                log.error( "No CAPTCHA implementation found for class " + captchaClassName, e );
            }
        }

        Captcha captcha = null;
        if( captchaClass != null )
        {
            try
            {
                captcha = (Captcha) captchaClass.newInstance();
            }
            catch( Exception e )
            {
                log.error( "Could not create CAPTCHA instance for class " + captchaClassName, e );
            }
        }
        return captcha;
    }
}
