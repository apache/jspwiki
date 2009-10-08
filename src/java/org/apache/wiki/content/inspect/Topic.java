/**
 * 
 */
package org.apache.wiki.content.inspect;

/**
 * A topic examined during an {@link Inspection}, for example "spam."
 */
public class Topic
{
    private final String m_topic;

    /**
     * Inspection topic for spam. Scores with negative scores are more likely to
     * be spam.
     */
    public static final Topic SPAM = new Topic( "spam" );

    /**
     * Constructs a new inspection topic.
     * 
     * @param topic the topic name
     */
    public Topic( String topic )
    {
        m_topic = topic;
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals( Object obj )
    {
        return obj instanceof Topic && m_topic.equals( ((Topic) obj).m_topic );
    }

    /**
     * Returns the name of the topic.
     * 
     * @return the topic name.
     */
    public String getTopic()
    {
        return m_topic;
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode()
    {
        return m_topic.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public String toString()
    {
        return "Topic[" + m_topic + "]";
    }
}
