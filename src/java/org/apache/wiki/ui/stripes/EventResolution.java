package org.apache.wiki.ui.stripes;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Message;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.ajax.JavaScriptBuilder;
import net.sourceforge.stripes.controller.StripesFilter;
import net.sourceforge.stripes.tag.ErrorsTag;
import net.sourceforge.stripes.tag.MessagesTag;
import net.sourceforge.stripes.validation.ValidationError;
import net.sourceforge.stripes.validation.ValidationErrors;

/**
 * <p>
 * Resolution that returns an {@code eval}-able set of JavaScript statements
 * that builds a variable containing the result of an AJAX call. The object is
 * called {@code eventResponse} and contains the call results, the
 * ActionBeanContext messages and validation errors. It has four properties:
 * </p>
 * <ul>
 * <li>{@code results} - the Object (or an array of Objects) that represents
 * the results of the AJAX call. The object or objects can be just about anything that
 * {@link net.sourceforge.stripes.ajax.JavaScriptBuilder} can encode.
 * It is most commonly a single HTML string.</li>
 * <li>{@code errors} - any global or field-level errors, as an HTML string. If
 * no errors, this property will be {@code null}</li>
 * <li>{@code messages} - any messages, as an HTML string. If no messages, this
 * property will be {@code null}</li>
 * </ul>
 * <p>
 * Error and message strings are generated in the same way that the Stripes
 * {@link net.sourceforge.stripes.tag.ErrorsTag} and
 * {@link net.sourceforge.stripes.tag.MessagesTag} work. For errors, the
 * contents of i18n message {@code stripes.errors.header} is prepended. Then,
 * for each error, the message {@code stripes.errors.beforeError} is added, then
 * the error text, then {@code stripes.errors.afterError}. Finally, the message
 * {@code stripes.errors.footer} is appended. For messages, the string is
 * generated the same way except that the messages {@code
 * stripes.messages.header}, {@code stripes.messages.beforeMessage}, {@code
 * stripes.messages.afterMessage} and {@code stripes.messages.footer} are used.
 * </p>
 */
public class EventResolution implements Resolution
{
    /**
     * Lightweight class that represents the result of an AJAX call, including
     * any Stripes messages or validation errors.
     */
    public static class Result
    {
        private final String m_errors;

        private final String m_messages;

        private final Object m_rootObject;

        /**
         * Constructs a new EventResolution.
         * 
         * @param context the ActionBeanContext that supplies the messages and
         *            validation errors.
         * @param objects zero or more Objects to be returned
         */
        public Result( ActionBeanContext context, Object... objects )
        {
            super();

            // Set the messages
            m_messages = context.getMessages().size() > 0 ? generateMessages( context ) : null;

            // Set the validation errors
            m_errors = context.getValidationErrors().size() > 0 ? generateErrors( context ) : null;

            // Set the root object
            if ( objects.length == 1 )
            {
                m_rootObject = objects[0];
            }
            else if ( objects.length > 0 )
            {
                m_rootObject = objects;
            }
            else
            {
                m_rootObject = null;
            }
        }

        /**
         * Return the errors set by the event.
         * 
         * @return the errors
         */
        public String getErrors()
        {
            return m_errors;
        }

        /**
         * Return the messages set by the event.
         * 
         * @return the messages
         */
        public String getMessages()
        {
            return m_messages;
        }

        /**
         * Returns the result of the AJAX call. If no objects were passed
         * to the constructor, this method returns {@code null}. If one
         * was passed, that object will be returned. Otherwise, an array of
         * objects will be returned.
         * 
         * @return the root object; {@code null}, one object, or an array of objects
         */
        public Object getResults()
        {
            return m_rootObject;
        }

        /**
         * Generates an HTML-formatted String representing the errors contained by
         * an ActionBeanContext. Global errors will be first, followed by
         * field-level errors. The contents will be localized according to the
         * user's locale.
         * 
         * @param context the context
         * @return the form
         */
        private String generateErrors( ActionBeanContext context )
        {
            Locale locale = context.getLocale();
            ResourceBundle bundle = StripesFilter.getConfiguration().getLocalizationBundleFactory().getErrorMessageBundle( locale );

            // Fetch the error headers and footers
            String header = getMessage( bundle, "stripes.errors.header", ErrorsTag.DEFAULT_HEADER );
            String footer = getMessage( bundle, "stripes.errors.footer", ErrorsTag.DEFAULT_FOOTER );
            String beforeError = getMessage( bundle, "stripes.errors.beforeError", "<li>" );
            String afterError = getMessage( bundle, "stripes.errors.afterError", "</li>" );

            // Write out the error messages
            StringBuilder s = new StringBuilder();
            s.append( header );
            ValidationErrors errors = context.getValidationErrors();
            for( List<ValidationError> errorList : errors.values() )
            {
                for ( ValidationError error : errorList )
                {
                    s.append( beforeError );
                    s.append( error.getMessage( locale ) );
                    s.append( afterError );
                }
            }
            s.append( footer );
            return s.toString();
        }

        /**
         * Generates an HTML-formatted String representing the messages contained by
         * an ActionBeanContext. The contents will be localized according to the
         * user's locale.
         * 
         * @param context the context
         * @return the form
         */
        private String generateMessages( ActionBeanContext context )
        {
            Locale locale = context.getLocale();
            ResourceBundle bundle = StripesFilter.getConfiguration().getLocalizationBundleFactory().getErrorMessageBundle( locale );

            // Fetch the message headers and footers
            String header = getMessage( bundle, "stripes.messages.header", MessagesTag.DEFAULT_HEADER );
            String footer = getMessage( bundle, "stripes.messages.footer", MessagesTag.DEFAULT_FOOTER );
            String beforeMessage = getMessage( bundle, "stripes.messages.beforeMessage", "<li>" );
            String afterMessage = getMessage( bundle, "stripes.messages.afterMessage", "</li>" );

            // Write out the error messages
            StringBuilder s = new StringBuilder();
            s.append( header );
            for( Message message : context.getMessages() )
            {
                s.append( beforeMessage );
                s.append( message.getMessage( locale ) );
                s.append( afterMessage );
            }
            s.append( footer );
            return s.toString();
        }

        /**
         * Looks up a message in a MessageBundle with a given key. If the key is not
         * found in the ResourceBundle, a default value is returned. 
         * @param bundle the message bundle
         * @param key the message key to loop up
         * @param defaultValue the value to return if the message is not found
         * @return
         */
        private String getMessage( ResourceBundle bundle, String key, String defaultValue )
        {
            String value = null;
            try
            {
                value = bundle.getString( key );
            }
            catch( MissingResourceException mre )
            {
                value = defaultValue;
            }
            return value;
        }
    }

    private final JavaScriptBuilder m_builder;
    
    /**
     * Constructs a new EventResolution for a supplied ActionBeanContext and
     * result objects.
     * @param context the ActionBeanContext
     * @param objects zero, one or more objects that represent the result
     * of an {@link org.apache.wiki.ui.stripes.AjaxEvent}-annotated
     * event method
     */
    public EventResolution( ActionBeanContext context, Object... objects )
    {
        m_builder = new JavaScriptBuilder( new Result( context, objects ) );
    }

    /**
     * Converts the ActionBean messges, validation errors and root object passed
     * in to a series of JavaScript statements that reconstruct the
     * {@link Result} object in JavaScript, and store the object under the
     * variable name {@code eventResponse}.
     */
    public void execute( HttpServletRequest request, HttpServletResponse response ) throws Exception
    {
        response.setContentType( "text/javascript" );
        m_builder.setRootVariableName( "eventResponse" );
        m_builder.build( response.getWriter() );
        response.flushBuffer();
    }
}
