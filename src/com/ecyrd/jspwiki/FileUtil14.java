/* 
    JSPWiki - a JSP-based WikiWiki clone.

    Copyright (C) 2001-2003 Janne Jalkanen (Janne.Jalkanen@iki.fi)

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU Lesser General Public License as published by
    the Free Software Foundation; either version 2.1 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Lesser General Public License for more details.

    You should have received a copy of the GNU Lesser General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package com.ecyrd.jspwiki;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;

import org.apache.log4j.Category;

/**
 *  Generic utilities related to file and stream handling, JDK1.4 version.
 *  Do not call this directly - go through FileUtil, since it is smart enough
 *  to decide which version you want to call.
 *  <p>
 *  This class contains only JDK1.4 -specific methods.
 */
// FIXME: It would be so much neater to do a clean subclassing here
// but since this is a static class, we'd need to do some kind of
// redirection.  For later.
public class FileUtil14
{
    private static final Category   log = Category.getInstance(FileUtil.class);

    /**
     *  JDK 1.4 version of FileUtil.readContents.  This version circumvents all kinds
     *  of problems just by gulping in the entire inputstream to a ByteArray.
     */
    public static String readContents( InputStream input, String encoding )
        throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        FileUtil.copyContents( input, out );

        ByteBuffer     bbuf        = ByteBuffer.wrap( out.toByteArray() );

        Charset        cset        = Charset.forName( encoding );
        CharsetDecoder csetdecoder = cset.newDecoder();

        csetdecoder.onMalformedInput( CodingErrorAction.REPORT );
        csetdecoder.onUnmappableCharacter( CodingErrorAction.REPORT );

        try
        {
            CharBuffer cbuf = csetdecoder.decode( bbuf );

            return cbuf.toString();
        }
        catch( CharacterCodingException e )
        {
            Charset        latin1    = Charset.forName("ISO-8859-1");
            CharsetDecoder l1decoder = latin1.newDecoder();

            l1decoder.onMalformedInput( CodingErrorAction.REPORT );
            l1decoder.onUnmappableCharacter( CodingErrorAction.REPORT );

            try
            {
                bbuf = ByteBuffer.wrap( out.toByteArray() );

                CharBuffer cbuf = l1decoder.decode( bbuf );

                return cbuf.toString();
            }
            catch( CharacterCodingException ex )
            {
                throw (CharacterCodingException) ex.fillInStackTrace();
            }
        }
    }

    public static String getThrowingMethod( Throwable t )
    {
        StackTraceElement[] trace = t.getStackTrace();
        StringBuffer sb = new StringBuffer();
        
        if( trace == null || trace.length == 0 ) {
            sb.append( "[Stack trace not available]" );
        } else {
            sb.append( trace[0].isNativeMethod() ? "native method" : "" );
            sb.append( trace[0].getClassName() );
            sb.append(".");
            sb.append(trace[0].getMethodName()+"(), line "+trace[0].getLineNumber());
        }
        return sb.toString();
    }
}
