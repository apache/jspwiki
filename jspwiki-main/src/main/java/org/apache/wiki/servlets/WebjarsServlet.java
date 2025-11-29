/**
 * MIT licensed from https://github.com/webjars/webjars-servlet-2.x
 */
package org.apache.wiki.servlets;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * FORK of https://github.com/webjars/webjars-servlet-2.x to update to the jakarta spec
 * 
 *
 * <p>This servlet enables Servlet 2.x compliant containers to serve up Webjars resources</p>
 * <p>To use it just declare it in your web.xml as follows:</p>
 * <pre>
 &lt;!--Webjars Servlet--&gt;
 &lt;servlet&gt;
     &lt;servlet-name&gt;WebjarsServlet&lt;/servlet-name&gt;
     &lt;servlet-class&gt;org.webjars.servlet.WebjarsServlet&lt;/servlet-class&gt;
 &lt;/servlet&gt;
 &lt;servlet-mapping&gt;
     &lt;servlet-name&gt;WebjarsServlet&lt;/servlet-name&gt;
     &lt;url-pattern&gt;/webjars/*&lt;/url-pattern&gt;
 &lt;/servlet-mapping&gt;œ
 </pre>
 * <p>It will automatically detect the webjars-locator-core library on the classpath and use it to automatically resolve
 * the version of any WebJar assets</p>
 * @author Angel Ruiz&lt;aruizca@gmail.com&gt;
 * @author Jaco de Groot&lt;jaco@wearefrank.nl&gt;
 */
public class WebjarsServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final Logger logger = Logger.getLogger(WebjarsServlet.class.getName());
    
    private static final long DEFAULT_EXPIRE_TIME_MS = 86400000L; // 1 day
    private static final long DEFAULT_EXPIRE_TIME_S = 86400L; // 1 day

    private boolean disableCache = false;

	private Object webJarAssetLocator;
	private Method getFullPathExact;

    @Override
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void init() throws ServletException {
        ServletConfig config = getServletConfig();
        if(config == null) {
            throw new NullPointerException("Expected servlet container to provide a non-null ServletConfig.");
        }
        try {
            String disableCache = config.getInitParameter("disableCache");
            if (disableCache != null) {
                this.disableCache = Boolean.parseBoolean(disableCache);
                logger.log(Level.INFO, "WebjarsServlet cache enabled: {0}", !this.disableCache);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "The WebjarsServlet configuration parameter \"disableCache\" is invalid");
        }
        try {
            Class webJarAssetLocatorClass = Class.forName("org.webjars.WebJarAssetLocator");
            webJarAssetLocator = webJarAssetLocatorClass.newInstance();
            getFullPathExact = webJarAssetLocatorClass.getMethod("getFullPathExact", String.class, String.class);
            logger.log(Level.INFO, "The webjars-locator-core library is present, WebjarsServlet will try to resolve the version of requested WebJar assets (for the version agnostic way of working)");
        } catch (Exception e) {
            logger.log(Level.INFO, "The webjars-locator-core library is not present, WebjarsServlet will not try to resolve the version of requested WebJar assets (for the version agnostic way of working)");
        }
        logger.log(Level.INFO, "WebjarsServlet initialization completed");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String webjarsURI = request.getRequestURI().replaceFirst(request.getContextPath(), "");
        String webjarsResourceURI = "/META-INF/resources" + webjarsURI;
        logger.log(Level.FINE, "Webjars resource requested: {0}", webjarsResourceURI);

        if (isDirectoryRequest(webjarsResourceURI)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        if (webJarAssetLocator != null) {
            String path = webjarsURI.substring(request.getServletPath().length());
            logger.log(Level.FINE, "Try to resolve version for path: {0}", path);
            // See also Spring's WebJarsResourceResolver findWebJarResourcePath() method
            int startOffset = (path.startsWith("/") ? 1 : 0);
            int endOffset = path.indexOf('/', 1);
            if (endOffset != -1) {
                String webjar = path.substring(startOffset, endOffset);
                String partialPath = path.substring(endOffset + 1);
                String webJarPath = null;
                try {
                    webJarPath = (String)getFullPathExact.invoke(webJarAssetLocator, webjar, partialPath);
                } catch (Exception e) {
                    logger.log(Level.FINE, "This should not happen", e);
                }
                if (webJarPath != null) {
                    webjarsResourceURI = "/" + webJarPath;
                }
            }
        }

        String eTagName;
        try {
            eTagName = this.getETagName(webjarsResourceURI);
        } catch (IllegalArgumentException e) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        if (!disableCache) {
            if (checkETagMatch(request, eTagName)
                   || checkLastModify(request)) {
               // response.sendError(HttpServletResponse.SC_NOT_MODIFIED); 
               response.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
               return;
            }
        }

        InputStream inputStream = this.getClass().getResourceAsStream(webjarsResourceURI);
        if (inputStream != null) {
            try {
                if (!disableCache) {
                    prepareCacheHeaders(response, eTagName);
                }
                String filename = getFileName(webjarsResourceURI);
                String mimeType = this.getServletContext().getMimeType(filename);

                response.setContentType(mimeType != null ? mimeType : "application/octet-stream");
                copy(inputStream, response.getOutputStream());
            } finally {
                inputStream.close();
            }
        } else {
            // return HTTP error
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private static boolean isDirectoryRequest(String uri) {
        return uri.endsWith("/");
    }

    /**
     *
     * @param webjarsResourceURI
     * @return
     */
    private String getFileName(String webjarsResourceURI) {
        String[] tokens = webjarsResourceURI.split("/");
        return tokens[tokens.length - 1];
    }

    
    /**
     * 
     * @param webjarsResourceURI
     * @return
     * @throws IllegalArgumentException when insufficient URI has given
     */
    private String getETagName(String webjarsResourceURI) {
    	
    	String[] tokens = webjarsResourceURI.split("/");
        if (tokens.length < 7) {
            throw new IllegalArgumentException("insufficient URL has given: " + webjarsResourceURI);
        }
        String version = tokens[5];
        String fileName = tokens[tokens.length - 1];

        String eTag = fileName + "_" + version;
        return eTag;
    }
    
    /**
     * 
     * @param request
     * @param eTagName
     * @return
     */
    private boolean checkETagMatch(HttpServletRequest request, String eTagName) {
    
       String token = request.getHeader("If-None-Match");
       return (token == null ? false: token.equals(eTagName));
    }

    /**
     * 
     * @param request
     * @return
     */
    private boolean checkLastModify(HttpServletRequest request) {
    	
       long last = request.getDateHeader("If-Modified-Since");
       return (last == -1L? false : (last - System.currentTimeMillis() > 0L));
    }
    
    /**
     *
     * @param response
     * @param webjarsResourceURI
     */
    private void prepareCacheHeaders(HttpServletResponse response, String eTag) {
        
        response.setHeader("ETag", eTag);
        response.setDateHeader("Expires", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME_MS);
        response.addDateHeader("Last-Modified", System.currentTimeMillis() + DEFAULT_EXPIRE_TIME_MS); 
        response.addHeader("Cache-Control", "private, max-age=" + DEFAULT_EXPIRE_TIME_S);
    }

        /* Important!!*/
    /* The code bellow has been copied from apache Commons IO. More specifically from its IOUtils class. */
    /* The reason is becasue I don't want to include any more dependencies */

    /**
     * The default buffer size ({@value}) to use
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;

    private static final int EOF = -1;

    // copy from InputStream
    //-----------------------------------------------------------------------
    /**
     * Copy bytes from an <code>InputStream</code> to an
     * <code>OutputStream</code>.
     * <p>
     * This method buffers the input internally, so there is no need to use a
     * <code>BufferedInputStream</code>.
     * <p>
     * Large streams (over 2GB) will return a bytes copied value of
     * <code>-1</code> after the copy has completed since the correct
     * number of bytes cannot be returned as an int. For large streams
     * use the <code>copyLarge(InputStream, OutputStream)</code> method.
     *
     * @param input  the <code>InputStream</code> to read from
     * @param output  the <code>OutputStream</code> to write to
     * @return the number of bytes copied, or -1 if &gt; Integer.MAX_VALUE
     * @throws NullPointerException if the input or output is null
     * @throws IOException if an I/O error occurs
     * @since 1.1
     */
    private static int copy(InputStream input, OutputStream output) throws IOException {
        long count = 0;
        int n = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (EOF != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
            count += n;
        }
        if (count > Integer.MAX_VALUE) {
            return -1;
        }
        return (int) count;
    }
}