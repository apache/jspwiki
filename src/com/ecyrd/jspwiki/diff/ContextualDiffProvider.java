
package com.ecyrd.jspwiki.diff;

import java.io.IOException;
import java.util.Properties;
import java.util.Vector;

import org.apache.log4j.Logger;

import com.ecyrd.jspwiki.NoRequiredPropertyException;
import com.ecyrd.jspwiki.WikiEngine;


/**
 */
public class ContextualDiffProvider implements DiffProvider
{

    private static final Logger log = Logger.getLogger(ContextualDiffProvider.class);


    //TODO all of these publics can become jspwiki.properties entries...
    //TODO span title= can be used to get hover info...

    public int m_numberOfContexualElements = 0; //TODO 0 means all.

    public boolean m_emitChangeNextPreviousHyperlinks = true;

    //Don't use spans here the deletion and insertions are nested in this...
    public String m_changeStartHtml = ""; //This could be a image '>' for a start marker
    public String m_changeEndHtml = ""; //and an image for an end '<' marker

    public String m_insertionStartHtml = "<span class='diff-insertion'>";
    public String m_insertionEndHtml = "</span>";

    public String m_deletionStartHtml = "<span class='diff-deletion'>";
    public String m_deletionEndHtml = "</span>";


    private int m_changeNumber;
    private StringBuffer m_markupBuffer;
    private boolean m_moreChanges;


    public ContextualDiffProvider()
    {
    }


    /**
     * @see com.ecyrd.jspwiki.WikiProvider#getProviderInfo()
     */
    public String getProviderInfo()
    {
        return "TraditionalDiffProvider";
    }

    /**
     * @see com.ecyrd.jspwiki.WikiProvider#initialize(com.ecyrd.jspwiki.WikiEngine,
     *      java.util.Properties)
     */
    public void initialize(WikiEngine engine, Properties properties)
	throws NoRequiredPropertyException, IOException
    {
    }



    /**
     * Brain hurts, not sure of thread safety at this point, reexamine later.
     * @see com.ecyrd.jspwiki.diff.DiffProvider#makeDiffHtml(java.lang.String, java.lang.String)
     */
    public synchronized String makeDiffHtml(String wikiOld, String wikiNew)
    {
        //Might as well esacpe the whole thing once, handles &<>"
        String htmlizedOld = htmlize(wikiOld);
        String htmlizedNew = htmlize(wikiNew);
        
        //Sequencing handles lineterminator to <br /> and every-other consequtive space to a &nbsp;
        Object[] alpha = sequence(htmlizedOld);
        Object[] beta = sequence(htmlizedNew);
/*
        Diff diff = new Diff(alpha, beta);

        Diff.change changeNode = diff.diff_2(false);

        return createMarkup(alpha, beta, changeNode);
        */
        return "";
    }


    /** 
     * We aren't going to display our output in a PRE block, so work over the wiki text into something 
     * that will look reasonable.
     */
    private String htmlize(String wikiText)
    {
        String htmlized  = wikiText;
        
        htmlized = htmlized.replaceAll( "&", "&amp;");
        htmlized = htmlized.replaceAll( "<", "&lt;");
        htmlized = htmlized.replaceAll( ">", "&gt;");
        htmlized = htmlized.replaceAll( "\"", "&quot;");
                
        return htmlized; 
    }


    /**
     * Take the string and create an array from it, split it first on newlines, making 
     * sure to preserve the newlines in the elements, split each resulting element on 
     * spaces, preserving the spaces.
     * 
     * All this preseving of newlines and spaces is so the wikitext when diffed will have fidelity 
     * to it's original form.  As a side affect we see edits of purely whilespace.
     */
    private Object[] sequence(String wikiText)
    {
        Vector list = new Vector();
        String[] linesArray = wikiText.split( "(\r\n|\r|\n)", -10 );
        for (int i = 0; i < linesArray.length; i++)
        {
            String line = linesArray[i];
            String[] wordArray = line.split(" ", -1000);
            
            String previousWord = "";
            for (int j = 0; j < wordArray.length; j++)
            {
                String word = wordArray[j];
                list.add(word + (previousWord.equals("") ? "&nbsp;" : " "));
                previousWord = word;
            }
            list.add("<br />");
        }
        
        String[] array = new String[list.size()];

        list.copyInto(array);

        return array;
    }


    /**
     * The real workhorse! Remember we are showing changes FROM Alpha TO Beta,
     * the names of line0, line1 leave something to be desired, but hey BMSI
     * diff does work nicely!
     */
    /*
    private String createMarkup(Object[] alphaSequence, Object[] betaSequence, Diff.change changeNode)
    {
        m_changeNumber = 0;
        m_markupBuffer = allocateMarkupBuffer(alphaSequence, betaSequence);

        m_markupBuffer.append("<div class='diff-wikitext'>");
        int currentAlphaPosition = 0;

        Diff.change someChange = changeNode;
        m_moreChanges = (null != someChange.link); 
        
        if (m_moreChanges)
            m_markupBuffer.append(changeHref(1, "Jump to first change &gt;&gt;") + "<br />");
        
        while (someChange != null)
        {
            appendSequenceElements(alphaSequence, currentAlphaPosition, someChange.line0);

            currentAlphaPosition = someChange.line0;

            handlePreChange();

            //If Deletion occured, show what was deleted from the Alpha
            // sequence...
            if (someChange.deleted != 0)
            {
                int sequenceEndPosition = currentAlphaPosition + someChange.deleted;
                
                handleSequenceDeletion(alphaSequence, currentAlphaPosition, sequenceEndPosition);
                
                currentAlphaPosition += someChange.deleted;
            }

            //If Insertion occured, show what was inserted into the Beta
            // sequence...
            if (someChange.inserted != 0)
            {
                int sequenceEndPosition = someChange.line1 + someChange.inserted;
                handleSequenceInsertion(betaSequence, someChange.line1, sequenceEndPosition);
            }

            handlePostChange();

            //Get the next change node...
            someChange = someChange.link;
            m_moreChanges = (null != someChange);
        }//while loop

        //Output the remains of the sequences out to the end.
        appendSequenceElements(alphaSequence, currentAlphaPosition, alphaSequence.length);

        m_markupBuffer.append("</div>"); 
        String markup = m_markupBuffer.toString();
        m_markupBuffer = null;

        return markup;
    }
*/
    private StringBuffer allocateMarkupBuffer(Object[] alphaSequence, Object[] betaSequence)
    {
        //This is really rough, but what the heck...
        int maxElements = Math.max(alphaSequence.length, betaSequence.length);
        int approxBufferSize = 20 * maxElements;

        return new StringBuffer(approxBufferSize);
    }


    /** Called once before each change is processed */
    void handlePreChange()
    {
        m_markupBuffer.append(m_changeStartHtml);

        if (m_emitChangeNextPreviousHyperlinks)
            m_markupBuffer.append(linkToPreviousChange());
    }


    /** Called once after each change is processed */
    void handlePostChange()
    {
        if (m_emitChangeNextPreviousHyperlinks && m_moreChanges)
            m_markupBuffer.append(linkToNextChange());

        m_markupBuffer.append(m_changeEndHtml);
    }


    private String linkToPreviousChange()
    {
        String changeAnchorName = "<a name='change-" + ( m_changeNumber + 1 ) + "'></a>";

        if (0 == m_changeNumber)
            return changeAnchorName;

        //TODO may be configurable image?
        String previousChangeHref = changeHref(m_changeNumber, "&lt;&lt;");;

        return changeAnchorName + previousChangeHref;
    }

    private String linkToNextChange()
    {
        m_changeNumber++;

        //TODO may be configurable image?
        String nextChangeHref = changeHref(m_changeNumber, "&gt;&gt;");

        return nextChangeHref;
    }


    private String changeHref(int changeNumber, String visualDohickey)
    {
        return "<a class='diff-nextprev' href='#change-" + changeNumber + "'> " + visualDohickey + "</a>";
    }


    /**
     * Called when a sequence deletion occured in the change. If both a deletion
     * and insertion occur in the same change, deletions are handled first.
     */
    private void handleSequenceDeletion(Object[] sequence, int fromIndex, int toIndex)
    {
        if (fromIndex < toIndex)
        {
            m_markupBuffer.append(m_deletionStartHtml);
            appendSequenceElements(sequence, fromIndex, toIndex);
            m_markupBuffer.append(m_deletionEndHtml);
        }
    }


    /**
     * Called when a sequence instertion occured in the change. If both a
     * deletion and insertion occur in the same change, deletions are handled
     * first.
     */
    private void handleSequenceInsertion(Object[] sequence, int fromIndex, int toIndex)
    {
        if (fromIndex < toIndex)
        {
            m_markupBuffer.append(m_insertionStartHtml);
            appendSequenceElements(sequence, fromIndex, toIndex);
            m_markupBuffer.append(m_insertionEndHtml);
        }
    }

    private void appendSequenceElements(Object[] sequence, int fromIndex, int toIndex)
    {
        while (fromIndex < toIndex)
        {
            m_markupBuffer.append(sequence[fromIndex].toString());
            fromIndex++;
        }
    }


    /**
     * TODO, this isn't used yet, until the props are done no real reason to get this ion there.
     * 
     * This implements the context word count limits, if 0 the whole sequence is output..
     */
    private void appendSequenceElements(Object[] sequence, int fromIndex, int toIndex, int elementSubset)
    {
        //Only adjust if potential range is wider and asked for...
        if ((toIndex - fromIndex) > (Math.abs(elementSubset)))
        {
            if (elementSubset < 0)
                fromIndex = toIndex + elementSubset;
    
            if (elementSubset > 0)
                toIndex = fromIndex + elementSubset;
        }
        
        while (fromIndex < toIndex)
        {
            m_markupBuffer.append(sequence[fromIndex].toString());
            fromIndex++;
        }
    }

}

