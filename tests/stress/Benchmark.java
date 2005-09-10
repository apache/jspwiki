package stress;

import org.apache.commons.lang.time.StopWatch;

public class Benchmark extends StopWatch
{
    /**
     *  How many operations/second?
     */
    public String toString( int operations )
    {
        double totalTime = getTime();

        return Double.toString( (operations/totalTime) * 1000.0 );
    }
}
