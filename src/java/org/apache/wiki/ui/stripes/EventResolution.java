package org.apache.wiki.ui.stripes;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Message;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.ajax.JavaScriptBuilder;
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
 * <li>{@code results} - the Object that contains the results of the AJAX call.
 * This can be just about anything that
 * {@link net.sourceforge.stripes.ajax.JavaScriptBuilder} can encode.</li>
 * <li>{@code fieldErrors} - any field-level errors</li>
 * <li>{@code globalErrors} - any global errors</li>
 * <li>{@code messages} - any messages</li>
 * <li>{@code isHtml} - whether the results should be treated as HTML (for
 * example, to inject directly into a {@code div})</li>
 * </ul>
 */
public class EventResolution implements Resolution
{
    public static class Result
    {
        private final List<ValidationError> m_globalErrors = new ArrayList<ValidationError>();

        private final List<ValidationError> m_fieldErrors = new ArrayList<ValidationError>();

        private final List<Message> m_messages;

        private final Object m_rootObject;

        private final boolean m_isHtml;

        /**
         * Constructs a new EventResolution.
         * 
         * @param context the ActionBeanContext that supplies the messages and
         *            validation errors.
         * @param rootObject the Object to be encoded
         * @param isHtml whether the results should be interpreted as HTML
         */
        public Result( ActionBeanContext context, Object rootObject, boolean isHtml )
        {
            super();

            // Set the messages
            m_messages = context.getMessages();

            // Set the validation errors
            ValidationErrors errors = context.getValidationErrors();
            Set<String> fields = errors.keySet();
            for( String field : fields )
            {
                List<ValidationError> fieldErrors = errors.get( field );
                if( fieldErrors != null )
                {
                    if( ValidationErrors.GLOBAL_ERROR.equals( field ) )
                    {
                        m_globalErrors.addAll( fieldErrors );
                    }
                    else
                    {
                        m_fieldErrors.addAll( fieldErrors );
                    }
                }
            }

            // Set the root object & HTML properties
            m_rootObject = rootObject;
            m_isHtml = isHtml;
        }

        /**
         * Return the messages set by the event.
         * 
         * @return the messages
         */
        public List<Message> getMessages()
        {
            return m_messages;
        }

        /**
         * Return the global ValidationErrors set by the event.
         * 
         * @return the errors
         */
        public List<ValidationError> getGlobalErrors()
        {
            return m_globalErrors;
        }

        /**
         * Return the field-level ValidationErrors set by the event.
         * 
         * @return the errors
         */
        public List<ValidationError> getFieldErrors()
        {
            return m_fieldErrors;
        }

        /**
         * Returns the root object.
         * 
         * @return the root object
         */
        public Object getResults()
        {
            return m_rootObject;
        }

        /**
         * Returns {@code true} if the result should be interpreted as HTML,
         * rather than as a JavaScript object that should be {@code eval}-ed.
         * 
         * @return the result
         */
        public boolean isHtml()
        {
            return m_isHtml;
        }
    }

    private final JavaScriptBuilder m_builder;

    public EventResolution( ActionBeanContext context, Object rootObject, boolean isHtml )
    {
        m_builder = new JavaScriptBuilder( new Result( context, rootObject, isHtml ) );
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
