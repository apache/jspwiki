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
import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import net.sourceforge.stripes.controller.ActionResolver;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.tag.FormTag;
import net.sourceforge.stripes.util.CryptoUtil;

import org.apache.wiki.WikiEngine;
import org.apache.wiki.action.WikiActionBean;
import org.apache.wiki.content.inspect.*;
import org.apache.wiki.filters.SpamFilter;
import org.apache.wiki.ui.stripes.HandlerInfo;
import org.apache.wiki.ui.stripes.SpamInterceptor;

/**
 * <p>
 * Tag that injects hidden {@link SpamFilter}-related parameters into the
 * current form, which will be parsed and verified by {@link SpamInterceptor}
 * whenever the input is processed by an ActionBean event handler method
 * annotated with the {@link org.apache.wiki.ui.stripes.SpamProtect} annotation.
 * If a CAPTCHA test is required, the required content will be written to the
 * page also, based on the results of
 * {@link Captcha#formContent(org.apache.wiki.content.inspect.Inspection)}.
 * </p>
 * <p>
 * This tag must be added as a child of an existing &lt;form&gt; or
 * &lt;stripes:form&gt; element. If The SpamProtect tag will cause the following
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
 * before it can send syntactically correct POSTs. This is a LOT more effort
 * than just simply looking at the fields once and crafting your auto-poster to
 * conform.</li>
 * <li><b>An empty, input field hidden with CSS</b>. This parameter is meant to
 * catch bots which do a GET and then randomly fill all fields with garbage.
 * This field <em>must</em>be empty when SpamFilter examines the contents of the
 * POST. Since it's hidden with the use of CSS, the bot would need to understand
 * CSS to bypass this check. Because the parameter is also randomized, it
 * prevents bot authors from hard-coding the fact that it needs to be empty.</li>
 * </li>
 * <li><b>An an encrypted parameter</b> called
 * {@link BotTrapInspector#REQ_SPAM_PARAM}, whose contents are the names of
 * parameters 2 and 3, separated by a carriage return character. These contents
 * are then encrypted.</li>
 * <li><b>Any parameters needed by the configured {@link Captcha}</b>, if a
 * CAPTCHA is needed. The current CAPTCHA inspector's method
 * {@link Captcha#formContent(org.apache.wiki.WikiContext)} will be called to
 * generate the relevant form parameters or any other markup needed.</li>
 * </ol>
 * <p>
 * The idea between 2 & 3 is that SpamInterceptor expects two fields with random
 * names, one of which needs to be empty, and one of which needs to be filled
 * with pre-determined data. This is quite hard for most bots to catch, unless
 * they are specifically crafted for JSPWiki and contain some amount of logic to
 * figure out the scheme.
 * </p>
 */
public class SpamProtectTag extends WikiTagBase
{
    private static String getUniqueID()
    {
        StringBuilder sb = new StringBuilder();
        Random rand = new SecureRandom();

        for( int i = 0; i < 6; i++ )
        {
            char x = (char) ('A' + rand.nextInt( 26 ));

            sb.append( x );
        }

        return sb.toString();
    }

    /**
     * Writes the CAPTCHA form content and spam parameters to the curret
     * PageContext's JSPWriter. If a CAPTCHA is not needed, it will not
     * be written.
     */
    @Override
    public int doEndTag() throws JspException
    {
        try
        {
            writeCaptchaFormContent();
            writeSpamParams();
        }
        catch( IOException e )
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
     * Writes CAPTCHA-related form content to the current PageContext's
     * JSPWriter, if a CAPTCHA is needed.
     * 
     * @throws IOException
     */
    private void writeCaptchaFormContent() throws IOException
    {
        boolean needsCaptcha = false;
        Captcha captcha = findCaptcha();
        String captchaContent = null;
        if( captcha != null )
        {
            captchaContent = captcha.formContent( m_wikiContext );
            if( captchaContent != null )
            {
                needsCaptcha = true;
            }
        }
        JspWriter out = getPageContext().getOut();
        out.write( "<input name=\"" + CaptchaInspector.CAPTCHA_NEEDED_PARAM + "\" type=\"hidden\" value=\""
                   + CryptoUtil.encrypt( String.valueOf( Boolean.valueOf( needsCaptcha ) ) ) + "\" />\n" );
        if( needsCaptcha )
        {
            out.write( captchaContent );
        }
    }

    /**
     * Writes hidden spam-protection parameters to the current PageContext's
     * JSPWriter.
     * 
     * @throws IOException
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

    /**
     * Determines whether to use a CAPTCHA, based on WikiActionBean being used.
     * The WikiActionBean is determined by looking for a parent Stripes
     * {@link net.sourceforge.stripes.tag.FormTag}. If one is found, that
     * ActionBean is used (which makes sense, because that is where the form
     * will be posted to). If not, we use the current ActionBean returned by
     * {@code m_wikiActionBean} (usually a pretty good guess). If any event
     * handler methods for the ActionBean requires a CAPTCHA, the initialized
     * {@link Captcha} implementation will be returned.
     * 
     * @return the Captcha if one is required, or {@code null} is it is not
     *         needed for this ActionBean.
     */
    @SuppressWarnings( "unchecked" )
    protected Captcha findCaptcha()
    {
        Class<? extends WikiActionBean> actionBeanClass = null;

        // Try figuring out the ActionBean the enclosing FormTag will submit to
        FormTag tag = this.getParentTag( FormTag.class );
        if( tag != null )
        {
            // Figure out the ActionBean class
            String action = tag.getAction();
            ActionResolver resolver = StripesFilter.getConfiguration().getActionResolver();
            actionBeanClass = (Class<? extends WikiActionBean>) resolver.getActionBeanType( action );
        }

        // If that doesn't work, use the current ActionBean and handler event
        if( actionBeanClass == null )
        {
            actionBeanClass = m_wikiActionBean.getClass();
        }

        // Now that we know what ActionBean we're looking at, do we need a
        // Captcha for it?
        Map<Method, HandlerInfo> events = HandlerInfo.getHandlerInfoCollection( actionBeanClass );
        for( Map.Entry<Method, HandlerInfo> entry : events.entrySet() )
        {
            if( entry.getValue().getCaptchaPolicy() == Captcha.Policy.ALWAYS )
            {
                WikiEngine engine = m_wikiActionBean.getContext().getEngine();
                InspectionPlan plan = SpamInspectionFactory.getInspectionPlan( engine, engine.getWikiProperties() );
                return plan.getCaptcha();
            }
        }
        return null;
    }
}
