/**
 * 
 */
package org.apache.wiki.content.inspect;

import javax.servlet.http.HttpServletRequest;

import org.apache.wiki.WikiContext;

/**
 * {@link Inspector} implementation that checks to see if the user's IP address
 * is on the list of banned addresses on the {@link ReputationManager}. If it
 * is, {@link #inspect(Inspection, String, Change)} will return a Finding with
 * {@link Finding.Result#FAILED}; otherwise {@code null}.
 */
public class BanListInspector implements Inspector
{
    public void initialize( InspectionPlan config )
    {
    }

    /**
     * Returns {@link Finding.Result#FAILED} if the IP address is banned;
     * {@code null} otherwise.
     * @param inspection the current Inspection
     * @param content the content that is being inspected
     * @param change the subset of the content that represents the added or
     *            deleted text since the last change
     * @return {@link Finding.Result#FAILED} if the test fails; {@code null} otherwise
     */
    public Finding[] inspect( Inspection inspection, String content, Change change )
    {
        ReputationManager banList = inspection.getPlan().getReputationManager();
        WikiContext context = inspection.getContext();
        HttpServletRequest req = context.getHttpRequest();
        if( req != null )
        {
            String remote = req.getRemoteAddr();
            long timeleft = banList.getRemainingBan( remote );
            if( timeleft != ReputationManager.NOT_BANNED )
            {
                return new Finding[] { new Finding( Topic.SPAM, Finding.Result.FAILED,
                                                    "You have been temporarily banned from modifying this wiki. (" + timeleft
                                                        + " seconds of ban left)" ) };
            }
        }
        return null;
    }
}
