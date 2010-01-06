/**
 * 
 */
package org.apache.wiki.content.inspect;

/**
 * Instructions to the parent {@link Inspection} object to increment, decrement
 * or reset the score for a particular topic. Each Finding is constructed with a
 * topic (for example, Topic.SPAM), a
 * {@link org.apache.wiki.content.inspect.Finding.Result} type that indicates
 * whether the test passed (which increases the score), failed (which decreases
 * it) or has no effect. An optional String message passed to the constructor
 * provides context for the change. For example, a Finding that would cause the
 * spam score to decrease (that is, be more likely to be spam) could be
 * constructed by <code>new Finding( Topic.SPAM,
 * Result.FAILED, "Bot detected." )</code>.
 */
public class Finding
{
    /**
     * The result of the {@link Inspector#inspect(Inspection, Change)}
     * method.
     */
    public static enum Result
    {
        /**
         * Indicates that a specific inspection passed, and that the overall
         * inspection score should be incremented by the weight assigned to the
         * Inspector that returned the Finding.
         */
        PASSED,

        /**
         * Indicates that a specific inspection failed, and that the overall
         * inspection score should be incremented by the weight assigned to the
         * Inspector that returned the Finding.
         */
        FAILED,

        /**
         * Indicates that a specific inspection returned no conclusive result,
         * and that the overall inspection score should stay the same.
         */
        NO_EFFECT,
    }

    private final Topic m_topic;

    private final String m_message;

    private final Finding.Result m_result;

    public Finding( Topic topic, Finding.Result result, String message )
    {
        if( topic == null || result == null || message == null )
        {
            throw new IllegalArgumentException( "Topic, result and message must be supplied." );
        }
        m_topic = topic;
        m_result = result;
        m_message = message;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals( Object obj )
    {
        if( !(obj instanceof Finding) )
        {
            return false;
        }
        Finding f = (Finding) obj;
        return m_topic.equals( f.m_topic ) && m_result.equals( f.m_result ) && m_message.equals( f.m_message );
    }

    public String getMessage()
    {
        return m_message;
    }

    public Finding.Result getResult()
    {
        return m_result;
    }

    public Topic getTopic()
    {
        return m_topic;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return m_topic.hashCode() + 7 * m_result.hashCode() + 31 * m_message.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "Finding[" + m_topic + ", " + m_result.toString() + ", \"" + m_message + "\"]";
    }
}
