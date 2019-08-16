/* 
    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); you may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.  
 */

package org.apache.wiki.util.comparators;

import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;

/**
 * A comparator that sorts Strings using "human" ordering, including decimal
 * ordering. Only works for languages where every character is lexigraphically
 * distinct and correctly unicode ordered (e.g. English). Other languages should use
 * <code>CollatedHumanComparator</code>. Pretty efficient but still slower than
 * String.compareTo().
 * 
 */
public class HumanComparator implements Comparator< String > {

    // Constants for categorising characters and specifying category level
    // ordering
    public enum CharType
    {
        TYPE_OTHER, TYPE_DIGIT, TYPE_LETTER
    }

    // A special singleton instance for quick access
    public static final Comparator<String> DEFAULT_HUMAN_COMPARATOR = new HumanComparator();

    /**
     * Returns a singleton comparator that implements the default behaviour.
     * 
     * @return the singleton comparator.
     */
    public static Comparator<String> getInstance()
    {
        return DEFAULT_HUMAN_COMPARATOR;
    }

    private CharType[] sortOrder = { CharType.TYPE_OTHER, CharType.TYPE_DIGIT, CharType.TYPE_LETTER };

    /**
     * Default constructor which does nothing. Here because it has a non-default
     * constructor.
     */
    public HumanComparator()
    {
        // Empty
    }

    /**
     * Constructor specifying all the character type order.
     * 
     * @param sortOrder see setSortOrder
     */
    public HumanComparator( CharType[] sortOrder )
    {
        setSortOrder( sortOrder );
    }

    /*
     * (non-Javadoc)
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare( String str1, String str2 )
    {
        // Some quick and easy checks
        if( StringUtils.equals( str1, str2 ) ) {
            // they're identical, possibly both null
            return 0;
        } else if ( str1 == null ) {
            // str1 is null and str2 isn't so str1 is smaller
            return -1;
        } else if ( str2 == null ) {
            // str2 is null and str1 isn't so str1 is bigger
            return 1;
        }

        char[] s1 = str1.toCharArray();
        char[] s2 = str2.toCharArray();
        int len1 = s1.length;
        int len2 = s2.length;
        int idx = 0;
        // caseComparison used to defer a case sensitive comparison
        int caseComparison = 0;

        while ( idx < len1 && idx < len2 )
        {
            char c1 = s1[idx];
            char c2 = s2[idx++];

            // Convert to lower case
            char lc1 = Character.toLowerCase( c1 );
            char lc2 = Character.toLowerCase( c2 );

            // If case makes a difference, note the difference the first time
            // it's encountered
            if( caseComparison == 0 && c1 != c2 && lc1 == lc2 )
            {
                if( Character.isLowerCase( c1 ) )
                    caseComparison = 1;
                else if( Character.isLowerCase( c2 ) )
                    caseComparison = -1;
            }
            // Do the rest of the tests in lower case
            c1 = lc1;
            c2 = lc2;

            // leading zeros are a special case
            if( c1 != c2 || c1 == '0' )
            {
                // They might be different, now we can do a comparison
                CharType type1 = mapCharTypes( c1 );
                CharType type2 = mapCharTypes( c2 );

                // Do the character class check
                int result = compareCharTypes( type1, type2 );
                if( result != 0 )
                {
                    // different character classes so that's sufficient
                    return result;
                }

                // If they're not digits, use character to character comparison
                if( type1 != CharType.TYPE_DIGIT )
                {
                    Character ch1 = Character.valueOf( c1 );
                    Character ch2 = Character.valueOf( c2 );
                    return ch1.compareTo( ch2 );
                }

                // The only way to get here is both characters are digits
                assert( type1 == CharType.TYPE_DIGIT && type2 == CharType.TYPE_DIGIT );
                result = compareDigits( s1, s2, idx - 1 );
                if( result != 0 )
                {
                    // Got a result so return it
                    return result;
                }

                // No result yet, spin through the digits and continue trying
                while ( idx < len1 && idx < len2 && Character.isDigit( s1[idx] ) ) {
                	idx++;
                }
            }
        }

        if( len1 == len2 )
        {
            // identical so return any case dependency
            return caseComparison;
        }

        // Shorter String is less
        return len1 - len2;
    }

    /**
     * Implements ordering based on broad categories (e.g. numbers are always
     * less than digits)
     * 
     * @param type1 first CharType
     * @param type2 second CharType
     * @return -1 if type1 < type2, 0 if type1 == type2, 1 if type1 > type2
     */
    private int compareCharTypes( CharType type1, CharType type2 )
    {
        if( type1 == type2 )
        {
            // Same type so equal
            return 0;
        } else if( type1 == sortOrder[0] )
        {
            // t1 is the lowest order and t2 isn't so t1 must be less
            return -1;
        } else if( type2 == sortOrder[0] )
        {
            // t2 is the lowest order and t1 isn't so t1 must be more
            return 1;
        } else if( type1 == sortOrder[1] )
        {
            // t1 is the middle order and t2 isn't so t1 must be less
            return -1;
        } else if( type2 == sortOrder[1] )
        {
            // t2 is the middle order and t1 isn't so t1 must be more
            return 1;
        } else
        {
            // Can't possibly get here as that would mean they're both sortOrder[2]
            assert( type1 != type2 );
            return 0;
        }
    }

    /**
     * Do a numeric comparison on two otherwise identical char arrays.
     * 
     * @param left the left hand character array.
     * @param offset the index of the first digit of the number in both char
     *            arrays.
     * @return negative, zero or positive depending on the numeric comparison of
     *         left and right.
     */
    private int compareDigits( char[] left, char[] right, int offset )
    {
        // Calculate the integer value of the left hand side
        int idx = offset;
        while ( idx < left.length && Character.isDigit( left[idx] ) ) {
        	idx++;
        }
        int leftLen = idx - offset;
        int leftValue = Integer.valueOf( new String( left, offset, leftLen ) );

        // Calculate the integer value of the right hand side
        idx = offset;
        while ( idx < right.length && Character.isDigit( right[idx] ) ) {
        	idx++;
        }
        int rightLen = idx - offset;
        int rightValue = Integer.valueOf( new String( right, offset, rightLen ) );

        if( leftValue == rightValue ) {
            return leftLen - rightLen; // Same value so use the lengths
        }
        return leftValue - rightValue; // Otherwise compare the values
    }

    public CharType[] getSortOrder()
    {
        return sortOrder;
    }

    /**
     * Very broadly characterises a character as a digit, a letter or a punctuation character.
     * 
     * @param c <code>char</code> to be characterised
     * @return <code>IS_DIGIT</code> if it's a digit, <code>IS_LETTER</code> if
     *         it's a letter, <code>IS_PUNC</code> otherwise.
     */
    private CharType mapCharTypes( char c ) {
        if( Character.isDigit( c ) ) {
            return CharType.TYPE_DIGIT;
        } else if( Character.isLetter( c ) ) {
            return CharType.TYPE_LETTER;
        } else {
            return CharType.TYPE_OTHER;
        }
    }

    /**
     * Set the order in which letters, numbers and everything else is presented.
     * Default is other, digits and then letters. For example, the strings
     * "abb", "a1b" and "a b" will sort in the order "a b", "a1b" then "abb" by
     * default.
     * 
     * @param sortOrder Must be an array of <code>CharType</code> containing
     *            exactly 3 elements each of which must be distinct.
     * @throws IllegalArgumentException if being called on the result of
     *             <code>HumanStringComparator.getInstance()</code> or
     *             <code>sortOrder</code> is not exactly 3 different
     *             <code>CharType</code>.
     */
    public void setSortOrder( CharType[] sortOrder ) {
        if( this == DEFAULT_HUMAN_COMPARATOR ) {
        	throw new IllegalArgumentException( "Can't call setters on default " + HumanComparator.class.getName() );
        }

        // Sanity check the sort order
        if( sortOrder == null || sortOrder.length != 3 ) {
            throw new IllegalArgumentException( "There must be exactly three elements in the sort order" );
        }
        if( sortOrder[0] == sortOrder[1] || sortOrder[0] == sortOrder[2] || sortOrder[1] == sortOrder[2] ) {
            throw new IllegalArgumentException( "The sort order must contain EXACTLY one of each CharType" );
        }
        this.sortOrder = sortOrder.clone();
    }

}
