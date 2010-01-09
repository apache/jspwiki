/* 
    JSPWiki - a JSP-based WikiWiki clone.

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
package org.apache.wiki.tags;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import net.sourceforge.stripes.util.CryptoUtil;
import net.sourceforge.stripes.validation.LocalizableError;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.api.WikiException;
import org.apache.wiki.content.inspect.*;
import org.apache.wiki.filters.SpamFilter;
import org.apache.wiki.ui.stripes.SpamInterceptor;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;

/**
 * <p>
 * Tag that injects hidden {@link SpamFilter}-related parameters into the
 * current form, which will be parsed and verified by {@link SpamInterceptor}
 * whenever the input is processed by an ActionBean event handler method
 * annotated with the {@link org.apache.wiki.ui.stripes.SpamProtect} annotation.
 * If a Challenge test is required, the required content will be written to the
 * page also, based on the results of
 * {@link Challenge#formContent(WikiActionBeanContext)}.
 * </p>
 * <p>
 * This tag has one optional; attribute, {@code challenge}. If supplied, a
 * {@link Challenge} will be rendered in the format specified. The value {@code
 * captcha} indicates that a CAPTCHA will be rendered using the CAPTCHA object
 * configured for the WikiEngine. The value {@code password} indicates that the
 * user must supply their password. The password option is only available if the
 * user is already logged in, and JSPWiki is using built-in authentication. If
 * container authentication is used or if the user is not logged in, the {@code
 * password} will be ignored. If {@code challenge} is not supplied, a CAPTCHA
 * will be generated on-demand if {@link SpamInterceptor} determined that the
 * ActionBean contains spam.
 * </p>
 * <p>
 * This tag must be added as a child of an existing &lt;form&gt; or
 * &lt;stripes:form&gt; element. The SpamProtect tag will cause the following
 * parameters into the form:
 * </p>
 * <ol>
 * <li><b>An UTF-8 detector parameter called <code>encodingcheck</code>.</b>
 * This parameter contains a single non-Latin1 character. SpamInterceptor
 * verifies that the non-Latin1 character has not been mangled by a badly
 * behaving robot or user client. Many bots assume a form is Latin1. This also
 * prevents the "hey, my edit destroyed all UTF-8 characters" problem.</li>
 * <li><b>A token field </b>, which has a random name and fixed value</b> This
 * means that a bot will need to actually GET the form first and parse it out
 * before it can send syntactically correct POSTs. This requires more effort
 * than just simply looking at the fields once and crafting your auto-poster to
 * conform.</li>
 * <li><b>An empty input field hidden with CSS</b>. This parameter is meant to
 * catch bots which do a GET and then randomly fill all fields with garbage.
 * This field <em>must</em>be empty when SpamFilter examines the contents of the
 * POST. Since it is hidden with the use of CSS, the bot would need to
 * understand CSS to bypass this check. Because the parameter is also
 * randomized, it prevents bot authors from hard-coding the fact that it needs
 * to be empty.</li> </li>
 * <li><b>An an encrypted parameter</b> called
 * {@link BotTrapInspector#REQ_SPAM_PARAM}, whose contents are the names of
 * parameters 2 and 3, separated by a carriage return character. These contents
 * are encrypted with a key stored server-side so that they cannot be tampered
 * with.</li>
 * <li><b>Any parameters needed by the configured {@link Challenge}</b>, if a
 * Challenge is needed. The appropriate Challenge inspector's method
 * {@link Challenge#formContent(WikiActionBeanContext)} will be called to
 * generate the relevant form parameters or any other markup needed.</li>
 * </ol>
 */
public class SpamProtectTag extends WikiTagBase
{
    private Challenge.State m_challenge = Challenge.State.CHALLENGE_NOT_PRESENTED;

    private static final String CHALLENGE_PASSWORD = "password";

    private static final String CHALLENGE_CAPTCHA = "captcha";

    private static final Random RANDOM = new SecureRandom();

    private static final Challenge PASSWORD_CHALLENGE = new PasswordChallenge();

    /**
     * Writes the Challenge form content and spam parameters to the current
     * PageContext's JSPWriter. If a Challenge is not needed, it will not be
     * written.
     * 
     * @throws IOException if content cannot be written to the JSPWriter
     */
    @Override
    public int doEndTag() throws JspException
    {
        try
        {
            writeChallengeFormContent();
            writeSpamParams();
        }
        catch( IOException e )
        {
            throw new JspException( e );
        }
        catch( WikiException e )
        {
            throw new JspException( e );
        }
        return super.doEndTag();
    }

    @Override
    public int doWikiStartTag() throws Exception
    {
        return TagSupport.SKIP_BODY;
    }

    /**
     * Sets the {@code challenge} attribute for this tag. Valid values are
     * {@code password} for {@link org.apache.wiki.content.inspect.Challenge.State#PASSWORD_PRESENTED} or {@code
     * captcha} for {@link org.apache.wiki.content.inspect.Challenge.State#CAPTCHA_PRESENTED}. If not supplied, the
     * challenge will default to {@link org.apache.wiki.content.inspect.Challenge.State#CHALLENGE_NOT_PRESENTED}.
     * 
     * @param challenge the type of challenge the user should see
     */
    public void setChallenge( String challenge )
    {
        if( CHALLENGE_CAPTCHA.equals( challenge.toLowerCase() ) )
        {
            m_challenge = Challenge.State.CAPTCHA_PRESENTED;
        }
        else if( CHALLENGE_PASSWORD.equals( challenge.toLowerCase() ) )
        {
            if ( !m_wikiContext.getEngine().getAuthenticationManager().isContainerAuthenticated() )
            {
                m_challenge = Challenge.State.PASSWORD_PRESENTED;
            }
        }
        else
        {
            m_challenge = Challenge.State.CHALLENGE_NOT_PRESENTED;
        }
    }

    /**
     * Generates a new 6-character unique identifier.
     * 
     * @return the unique ID
     */
    private String getUniqueID()
    {
        StringBuilder sb = new StringBuilder();
        for( int i = 0; i < 6; i++ )
        {
            char x = (char) ('A' + RANDOM.nextInt( 26 ));
            sb.append( x );
        }
        return sb.toString();
    }

    /**
     * Determines whether spam has been identified earlier in the request by
     * {@link SpamInterceptor}, based on the presence or absence of a global
     * {@link ValidationError} with key name
     * {@link SpamInterceptor#SPAM_VALIDATION_ERROR}.
     * @param actionBeanContext the ActionBeanContext
     * @return {@code true} if the ValidationError indicates that the
     *         ActionBean's contents are spam, or {@code false} is the contents
     *         are ok.
     */
    static public boolean isSpamDetected( WikiActionBeanContext actionBeanContext )
    {
        ValidationErrors errors = actionBeanContext.getValidationErrors();
        if ( errors.containsKey( ValidationErrors.GLOBAL_ERROR ) )
        {
            for( ValidationError error : errors.get( ValidationErrors.GLOBAL_ERROR ) )
            {
                if( error instanceof LocalizableError )
                {
                    LocalizableError localError = (LocalizableError) error;
                    if( SpamInterceptor.SPAM_VALIDATION_ERROR.equals( localError.getMessageKey() ) )
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Writes Challenge-related form content to the current PageContext's
     * JSPWriter, if a challenge is needed. The value of the {@code challenge}
     * attribute will always be written out as an encrypted parameter so that it
     * can be extracted by {@link SpamInterceptor} when the form is POSTed.
     * 
     * @throws IOException if content cannot be written to the JSPWriter
     */
    private void writeChallengeFormContent() throws IOException, WikiException
    {
        WikiEngine engine = m_wikiContext.getEngine();
        SpamInspectionPlan plan = SpamInspectionPlan.getInspectionPlan( engine );
        WikiActionBeanContext actionBeanContext = m_wikiActionBean.getContext();
        String challengeContent = null;

        switch( m_challenge )
        {
            case PASSWORD_PRESENTED: {
                challengeContent = PASSWORD_CHALLENGE.formContent( actionBeanContext );
                break;
            }
            case CAPTCHA_PRESENTED: {
                Captcha captcha = plan.getCaptcha();
                challengeContent = captcha.formContent( actionBeanContext );
                break;
            }
            case CHALLENGE_NOT_PRESENTED: {
                if( isSpamDetected( actionBeanContext ) )
                {
                    m_challenge = Challenge.State.CAPTCHA_PRESENTED;
                    Captcha captcha = plan.getCaptcha();
                    challengeContent = captcha.formContent( actionBeanContext );
                }
                break;
            }
        }

        // Always output the Challenge request parameter
        JspWriter out = getPageContext().getOut();
        out.write( "<input name=\"" + SpamInterceptor.CHALLENGE_REQUEST_PARAM + "\" type=\"hidden\" value=\""
                   + CryptoUtil.encrypt( String.valueOf( m_challenge.name() ) ) + "\" />\n" );

        // Output any generated Challenge content
        if( challengeContent != null )
        {
            out.write( challengeContent );
        }
    }

    /**
     * Writes hidden spam-protection parameters to the current PageContext's
     * JSPWriter.
     * 
     * @throws IOException if content cannot be written to the JSPWriter
     */
    private void writeSpamParams() throws IOException
    {
        JspWriter out = pageContext.getOut();
        HttpServletRequest request = (HttpServletRequest) this.pageContext.getRequest();

        // Inject honey-trap param (should always be submitted with no value)
        String trapParam = BotTrapInspector.REQ_TRAP_PARAM;
        out.write( "<div style=\"display: none;\">" );
        out.write( "<input type=\"hidden\" name=\"" );
        out.write( trapParam );
        out.write( "\" value=\"\" /></div>\n" );

        // Inject token field
        String tokenParam = getUniqueID();
        String tokenValue = request.getSession().getId();
        out.write( "<input name=\"" + tokenParam + "\" type=\"hidden\" value=\"" + tokenValue + "\" />\n" );

        // Inject UTF-8 detector
        out.write( "<input name=\"" + BotTrapInspector.REQ_ENCODING_CHECK + "\" type=\"hidden\" value=\"\u3041\" />\n" );

        // Add encrypted parameter indicating the name of the token field
        String encryptedParam = CryptoUtil.encrypt( tokenParam );
        out.write( "<input name=\"" + BotTrapInspector.REQ_SPAM_PARAM + "\" type=\"hidden\" value=\"" + encryptedParam + "\" />\n" );
    }
}
