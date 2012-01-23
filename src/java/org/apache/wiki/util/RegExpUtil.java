/*
    JSPWiki - a JSP-based WikiWiki clone.
    
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
package org.apache.wiki.util;

/**
 * Copied code from the org.apache.oro.text.regex.GlobCompiler
 * All credits to the author's
 */
public final class RegExpUtil
{
    private RegExpUtil()
    {
        // private constructor prevents construction
    }

    /**
     * The default mask . It is equal to 0
     * The default behavior is for a glob expression to be case sensitive
     * unless it is compiled with the CASE_INSENSITIVE_MASK option.
     */
    public static final int DEFAULT_MASK = 0;

    /**
     * A mask passed as an option to
     * indicate a compiled glob expression should be case insensitive.
     */
    public static final int CASE_INSENSITIVE_MASK = 0x0001;

    /**
     * A mask to indicate that a * should not be allowed to match the null string.
     * Thenormal behavior of the * metacharacter is that it may match any 0 or more
     * characters. This mask causes it to match 1 or more characters of
     * anything.
     */
    public static final int STAR_CANNOT_MATCH_NULL_MASK = 0x0002;

    /**
     * A mask to indicate that a ? should not be allowed to match the null string.
     * The normal behavior of the ? metacharacter is that it may match any 1
     * character. This mask causes it to match 0 or 1 characters.
     */
    public static final int QUESTION_MATCHES_ZERO_OR_ONE_MASK = 0x0004;

    /**
     * A mask to indicate that the resulting Perl5Pattern should be treated as a read only
     * data structure by Perl5Matcher, making it safe to share a single
     * Perl5Pattern instance among multiple threads without needing
     * synchronization. Without this option, Perl5Matcher reserves the right to
     * store heuristic or other information in Perl5Pattern that might
     * accelerate future matches. When you use this option, Perl5Matcher will
     * not store or modify any information in a Perl5Pattern. Use this option
     * when you want to share a Perl5Pattern instance among multiple threads
     * using different Perl5Matcher instances.
     */
    public static final int READ_ONLY_MASK = 0x0008;

    /**
     * This static method is the basic engine of the Glob PatternCompiler
     * implementation. It takes a glob expression in the form of a character
     * array and converts it into a String representation of a Perl5 pattern.
     * The method is made public so that programmers may use it for their own
     * purposes. However, the GlobCompiler compile methods work by converting
     * the glob pattern to a Perl5 pattern using this method, and then invoking
     * the compile() method of an internally stored Perl5Compiler instance.
     * <p>
     * 
     * @param pattern A character array representation of a Glob pattern.
     * @return A String representation of a Perl5 pattern equivalent to the Glob
     *         pattern.
     */
    public static String globToPerl5( char[] pattern, int options )
    {
        boolean inCharSet = false;
        boolean starCannotMatchNull = false;
        boolean questionMatchesZero = false;
        
        int ch;
        StringBuffer buffer;

        buffer = new StringBuffer( 2 * pattern.length );
        inCharSet = false;

        questionMatchesZero = (options & QUESTION_MATCHES_ZERO_OR_ONE_MASK) != 0;
        starCannotMatchNull = (options & STAR_CANNOT_MATCH_NULL_MASK) != 0;

        for( ch = 0; ch < pattern.length; ch++ )
        {
            switch( pattern[ch] )
            {
                case '*':
                    if( inCharSet )
                        buffer.append( '*' );
                    else
                    {
                        if( starCannotMatchNull )
                            buffer.append( ".+" );
                        else
                            buffer.append( ".*" );
                    }
                    break;
                case '?':
                    if( inCharSet )
                        buffer.append( '?' );
                    else
                    {
                        if( questionMatchesZero )
                            buffer.append( ".?" );
                        else
                            buffer.append( '.' );
                    }
                    break;
                case '[':
                    inCharSet = true;
                    buffer.append( pattern[ch] );

                    if( ch + 1 < pattern.length )
                    {
                        switch( pattern[ch + 1] )
                        {
                            case '!':
                            case '^':
                                buffer.append( '^' );
                                ++ch;
                                continue;
                            case ']':
                                buffer.append( ']' );
                                ++ch;
                                continue;
                        }
                    }
                    break;
                case ']':
                    inCharSet = false;
                    buffer.append( pattern[ch] );
                    break;
                case '\\':
                    buffer.append( '\\' );
                    if( ch == pattern.length - 1 )
                    {
                        buffer.append( '\\' );
                    }
                    else if( isGlobMetaCharacter( pattern[ch + 1] ) )
                        buffer.append( pattern[++ch] );
                    else
                        buffer.append( '\\' );
                    break;
                default:
                    if( !inCharSet && isPerl5MetaCharacter( pattern[ch] ) )
                        buffer.append( '\\' );
                    buffer.append( pattern[ch] );
                    break;
            }
        }

        return buffer.toString();
    }

    private static boolean isPerl5MetaCharacter( char ch )
    {
        return "'*?+[]()|^$.{}\\".indexOf( ch ) >= 0;
    }

    private static boolean isGlobMetaCharacter( char ch )
    {
        return "*?[]".indexOf( ch ) >= 0;
    }

}
