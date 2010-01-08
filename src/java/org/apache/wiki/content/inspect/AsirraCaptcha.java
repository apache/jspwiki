package org.apache.wiki.content.inspect;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import net.sourceforge.stripes.util.CryptoUtil;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.wiki.i18n.InternationalizationManager;
import org.apache.wiki.log.Logger;
import org.apache.wiki.log.LoggerFactory;
import org.apache.wiki.ui.stripes.WikiActionBeanContext;

/**
 * <p>
 * {@link Challenge} implementation for Microsoft's Asirra anti-bot testing
 * framework, version 3. Although Microsoft provides a handy client-side
 * JavaScript file that automates most of the test, it isn't very easy to
 * integrate without hard-coding it into JSPWiki forms. So, JSPWiki provides an
 * alternative implementation that cleanly integrates into the
 * content-inspection system in JSPWiki 3, and with the Stripes MVC layer.
 * </p>
 * <p>
 * But first, here's how Asirra works:
 * </p>
 * <ol>
 * <li>The first GET to Asirra establishes the Asirra session. The GET is a
 * simple URL with a nonce appended: <blockquote>
 * <code>http://challenge.asirra.com/cgi/Asirra?action=CreateSession&rand=0.8529005243601243</code>
 * </blockquote></li>
 * <li>The response to the GET is a JavaScript array with two items. The first
 * item, {@code asirraState.NewSessionComplete}, contains the Asirra version
 * number (usually 3) and the unique Asirra session ID. The second item, {@code
 * asirraState.GetChallengeComplete}, contains an array of twelve image URLs,
 * some of which are cats: <blockquote>
 * 
 * <pre>
 * asirraState.NewSessionComplete(3, &quot;2009354/ec2.9/419f480333bfa68f77d2bcf699fb90b0&quot;);
 * asirraState.GetChallengeComplete(
 * [ImgRec(&quot;3eb7628c26776e975f29a35db47a5c8f&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/3eb7628c26776e975f29a35db47a5c8f.jpg&quot;,250,250),
 * ImgRec(&quot;6f00c2a819ac8d474c528ae4381ae075&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/6f00c2a819ac8d474c528ae4381ae075.jpg&quot;,250,250),
 * ImgRec(&quot;729d2eb54e0a2a48ff3010cfad3222c0&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/729d2eb54e0a2a48ff3010cfad3222c0.jpg&quot;,250,250),
 * ImgRec(&quot;5af63b80c870e6c29083b3c35e684722&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/5af63b80c870e6c29083b3c35e684722.jpg&quot;,250,250),
 * ImgRec(&quot;ec95a8f0f67dee8ce37b8ee19931bf47&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/ec95a8f0f67dee8ce37b8ee19931bf47.jpg&quot;,250,250),
 * ImgRec(&quot;e2a52e27a19d72ab950fd47dde6fb1d3&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/e2a52e27a19d72ab950fd47dde6fb1d3.jpg&quot;,250,250),
 * ImgRec(&quot;9d6b79ae5f9be53631d9f9c8792d3ba7&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/9d6b79ae5f9be53631d9f9c8792d3ba7.jpg&quot;,250,250),
 * ImgRec(&quot;7f17ad775b6b45e705dc9cffd2ef1010&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/7f17ad775b6b45e705dc9cffd2ef1010.jpg&quot;,250,250),
 * ImgRec(&quot;ada0b1529793bf644ded00cd68659989&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/ada0b1529793bf644ded00cd68659989.jpg&quot;,250,250),
 * ImgRec(&quot;ca2da821c7aab041451dbe162cd2c60d&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/ca2da821c7aab041451dbe162cd2c60d.jpg&quot;,250,250),
 * ImgRec(&quot;2fcc9d2357bd6baac1bfe9452af3ab81&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/2fcc9d2357bd6baac1bfe9452af3ab81.jpg&quot;,250,250),
 * ImgRec(&quot;2a538185753c7b9f797d852c95b33af7&quot;,&quot;//s3.amazonaws.com/Asirra/PhotoDB/2a538185753c7b9f797d852c95b33af7.jpg&quot;,250,250)]
 * );
 * </pre>
 * 
 * </blockquote></li>
 * <li>After the user identifies which images are cats, the results are POSTed
 * back to the Asirra server. The {@code response} parameter contains a
 * 12-character string, where each character is {@code 0} or {@code 1}, with the
 * latter indicating a cat. The {@code sessionId} parameter contains the session
 * ID obtained from step 1. A random nonce is included also, similar to step 1.
 * <blockquote>
 * <code>http://challenge.asirra.com/cgi/Asirra?action=ScoreResponse&sessionId=2009354/ec2.9/419f480333bfa68f77d2bcf699fb90b0&response=001001100111&rand=0.2750088656556734</code>
 * </blockquote></li>
 * <li>The Asirra server response with the results of the test. A correct
 * response looks like this: <blockquote>
 * 
 * <code>asirraState.ScoreResponseComplete("3;2009354/ec2.9/419f480333bfa68f77d2bcf699fb90b0;02b4e5deb14a7177d36686cf5c8d2067");
&#47;* correct *&#47;</code></blockquote></li>
 * </ol>
 * <p>
 * This implementation splits processing of the Asirra test into two parts.
 * Method {@link #formContent(WikiActionBeanContext)}, establishes the Asirra session
 * (step 1 above), parses the response (step 2), extracts the session ID and
 * image URLs, and writes all of this information to the page. Method
 * {@link #check(WikiActionBeanContext)} extracts the user's answers, assembles them into a
 * response string, sends the response back to Asirra (step 3) and evaluates the
 * response (step 4).
 * </p>
 * <p>
 * This AsirraCaptcha class does all requests and responses using an
 * {@link HttpClient}, so there is no need for the user to load the client-side
 * Asirra JavaScript.
 * </p>
 */
public class AsirraCaptcha implements Challenge
{
    /**
     * Convenience class that encapsulates the image of a pet,
     * some of which are cats.
     */
    protected static class Pet
    {
        private final String m_id;

        private final String m_url;

        /**
         * Creates a new challenge image.
         * @param id the unique ID of the animal
         * @param url the URL of the animal's picture
         */
        public Pet( String id, String url )
        {
            m_id = id;
            m_url = url;
        }

        /**
         * Returns the unique ID of the animal
         * @return the unique ID
         */
        public String id()
        {
            return m_id;
        }

        /**
         * Returns the URL of the animal's picture
         * @return the URL
         */
        public String url()
        {
            return m_url;
        }
    }

    /**
     * Pattern for extracting session ID from challenge response e.g.,
     * 
     * <code>asirraState.NewSessionComplete(3, "2009352/ec2.8/33f46dd1299db9de910869db5a493ed2");</code>
     */
    protected static final Pattern SESSION_ID_PATTERN = Pattern.compile( "NewSessionComplete.+?\"(.+?)\"", Pattern.MULTILINE );

    /**
     * Pattern for extracting cats from challenge response e.g.,
     * 
     * <code>ImgRec("401906c8e5fb9117ee4e4666758a03af","//s3.amazonaws.com/Asirra/PhotoDB/401906c8e5fb9117ee4e4666758a03af.jpg",250,250)</code>
     * .
     */
    protected static final Pattern CATS_PATTERN = Pattern.compile( "ImgRec\\(\"(.+?)\",\"(.+?)\",", Pattern.MULTILINE );

    private static final String ASIRRA_ADOPT_ME_KEY = "org.apache.wiki.content.inspect.AsirraCaptcha.adoptMe";

    private static final String ASIRRA_DESCRIPTION_KEY = "org.apache.wiki.content.inspect.AsirraCaptcha.description";

    private static final String CAT_PARAM_PREFIX = "asirra_cat_";

    /**
     * Random session nonce generator. Does not need to be SecureRandom.
     */
    private static final Random RANDOM = new Random();

    private static final String SESSION_ID_PARAM = "asirra_session_id";

    /**
     * The URL used to obtain challenge responses, not including the random
     * number suffix.
     */
    protected static final String CHALLENGE_URL = "http://challenge.asirra.com/cgi/Asirra?action=CreateSession";

    protected static final String CHECK_URL = "http://challenge.asirra.com/cgi/Asirra?action=ScoreResponse";

    private static Logger log = LoggerFactory.getLogger( AsirraCaptcha.class );

    /**
     * Generates a parameter with a random nonce used with Asirra challenge/check requests.
     * @return {@code &rand=} plus the nonce
     */
    private static String random()
    {
        String nonce = String.valueOf( Math.abs( RANDOM.nextInt() ) );
        return "&rand=" + nonce;
    }

    /**
     * Extracts the Asirra challenge objects from the challenge response.
     * 
     * @param challengeResponse the challenge response
     * @return a list of {@link Pet} objects
     */
    protected static List<Pet> extractChallenges( String challengeResponse )
    {
        List<Pet> pets = new ArrayList<Pet>();
        Matcher matcher = CATS_PATTERN.matcher( challengeResponse );
        while ( matcher.find() )
        {
            pets.add( new Pet( matcher.group( 1 ), matcher.group( 2 ) ) );
        }
        return pets;
    }

    /**
     * Extracts the Asirra session ID from the challenge response.
     * 
     * @param challengeResponse the challenge response
     * @return the challenge ID, e.g., {@code
     *         2009352/ec2.8/33f46dd1299db9de910869db5a493ed2}
     */
    protected static String extractSessionId( String challengeResponse )
    {
        Matcher m = SESSION_ID_PATTERN.matcher( challengeResponse );
        if( m.find() )
        {
            return m.group( 1 );
        }
        return null;
    }

    /**
     * Calls out to Asirra and returns a new challenge response, which
     * will be shown to the user via {@link #formContent(WikiActionBeanContext)}.
     * @return the response from Asirra as a String
     * @throws IOException if Asirra cannot be contacted, or if the response
     * is malformed
     */
    protected static String getChallengeResponse() throws IOException
    {
        String challengeUrl = CHALLENGE_URL + random();
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod( challengeUrl );
        int status = client.executeMethod( method );
        if( status == HttpStatus.SC_OK )
        {
            return method.getResponseBodyAsString();
        }
        throw new IOException( "Response from Asirra was unexpected: " + status );
    }

    /**
     * Extracts the responses supplied by the user, assembles a response string,
     * and calls out to Asirra to validate the result.
     * @return {@code true} if Asirra agrees that the user has adopted only
     * cats, or {@code false} otherwise
     */
    public boolean check( WikiActionBeanContext actionBeanContext )
    {
        HttpServletRequest request = actionBeanContext.getRequest();

        // Get sessionId
        String encryptedSessionId = request.getParameter( SESSION_ID_PARAM );
        String sessionId = null;
        if( encryptedSessionId != null )
        {
            sessionId = CryptoUtil.decrypt( encryptedSessionId );
        }

        // Assemble the response string e.g., 000111000111
        String response = "";
        for( int i = 0; i < 12; i++ )
        {
            String cat = request.getParameter( CAT_PARAM_PREFIX + i );
            String singleResponse = (cat == null || !"1".equals( cat )) ? "0" : "1";
            response += singleResponse;
        }

        String submission = CHECK_URL + "&sessionId=" + sessionId + "&response=" + response + random();
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod( submission );

        try
        {
            int status = client.executeMethod( method );
            String body = method.getResponseBodyAsString();

            if( status == HttpStatus.SC_OK )
            {
                if( body.indexOf( "/* correct */" ) != -1 )
                {
                    return true;
                }
            }
        }
        catch( Exception e )
        {
            // Something bad happened
            log.error( "Could not execute Asirra CAPTHA: " + e.getMessage() );
            return false;
        }
        return false;
    }

    /**
     * Returns a &lt;script&gt; elements containing the JavaScript used to
     * generate the Asirra form. A &lt;SpamProtect&gt; tag will emit this
     * content if it specifies a value for the {@code content} attribute equal
     * to "form" or a null value.
     * 
     * @param context the current WikiActionBeanContext
     */
    public String formContent( WikiActionBeanContext context ) throws IOException
    {
        String challengeResponse = getChallengeResponse();
        String sessionId = extractSessionId( challengeResponse );
        List<Pet> pets = extractChallenges( challengeResponse );

        // Get the localized text
        Locale locale = context.getLocale();
        InternationalizationManager i18n = context.getEngine().getInternationalizationManager();
        String description = i18n.get( InternationalizationManager.CORE_BUNDLE, locale, ASIRRA_DESCRIPTION_KEY );
        String adoptMe = i18n.get( InternationalizationManager.CORE_BUNDLE, locale, ASIRRA_ADOPT_ME_KEY );
        
        StringBuilder b = new StringBuilder();
        b.append( "<input name=\"" + SESSION_ID_PARAM + 
                  "\" type=\"hidden\" value=\"" + CryptoUtil.encrypt( sessionId ) + "\" />\n" );
        b.append( "<div class=\"asirra\">" + description + "</div>" );
        b.append( "<table class=\"asirraCaptcha\">" );
        int i = 0;
        for( Pet pet : pets )
        {
            boolean firstInRow = i % 4 == 0;
            boolean lastInRow = (i + 1) % 4 == 0;
            if( firstInRow )
            {
                b.append( "<tr>" );
            }
            b.append( "<td>" );
            b.append( "<img src=\"http:" + pet.url() + "\" />" );
            b.append( "<br/>" );
            b.append( "<input type=\"checkbox\" name=\"" + CAT_PARAM_PREFIX + i + "\" value=\"1\">" );
            b.append( adoptMe );
            b.append( "</input>" );
            b.append( "</td>" );
            if( lastInRow )
            {
                b.append( "</tr>" );
            }
            i++;
        }
        b.append( "</table>" );
        return b.toString();
    }

    /**
     * Initializes the Asirra CAPTCHA. This method no-ops because
     * Asirra does not need any configuration.
     * @param config the inspection plan
     */
    public void initialize( InspectionPlan config )
    {
    }
}
