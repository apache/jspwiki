package stress;

public class Benchmark
{
    long m_start;
    long m_stop;

    public Benchmark()
    {
    }

    public final void start()
    {
        m_start = System.currentTimeMillis();
    }

    public final void stop()
    {
        m_stop = System.currentTimeMillis();
    }

    /**
     *  Returns duration in milliseconds.
     */
    public long getDurationMs()
    {
        return m_stop-m_start;
    }

    /**
     *  Returns seconds.
     */
    public String toString()
    {
        return Double.toString( ((double)m_stop - (double)m_start) / 1000.0 );
    }
    
    /**
     *  How many operations/second?
     */
    public String toString( int operations )
    {
        double totalTime = (double)m_stop - (double)m_start;

        return Double.toString( (operations/totalTime) * 1000.0 );
    }
}
