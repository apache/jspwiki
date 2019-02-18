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

package org.apache.wiki.util;

import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

/**
 *  A utility class to generate XHTML objects and ultimately, serialised markup.
 *  This class is incomplete but serves as a basic utility for JSPWiki, to be
 *  expanded upon as needed.
 *  <p>
 *  This uses JDOM2 as its backing implementation.
 *  </p>
 *
 *  <h3>Example</h3>
 *  <p>
 *  To generate a single element, an Element with PCDATA content, and then
 *  embed the latter in the former:
 *  </p>
 *  <pre>
 *    Element div = XhtmlUtil.element(XHTML.div);
 *    Element p   = XhtmlUtil.element(XHTML.p,"Some content");
 *    div.addContent(p);
 *  </pre>
 *  <p>
 *  There is also a convenient link and link target constructor methods:
 *  </p>
 *  <pre>
 *    Element link   = XhtmlUtil.link("hrefValue","linkText");
 *    Element target = XhtmlUtil.target("targetIdValue","linkText");
 *  </pre>
 *
 * @since 2.10
 */
public final class XhtmlUtil {

    private XhtmlUtil() {}

    /** to print <td></td> instead of <td /> */
    public static final Format EXPAND_EMPTY_NODES = Format.getCompactFormat().setExpandEmptyElements( true );

    /**
     *  Serializes the Element to a String using a compact serialization format.
     *
     * @param element  the element to serialize.
     * @return the serialized Element.
     */
    public static String serialize( Element element ) {
        return serialize( element, false );
    }

    /**
     *  Serializes the Element to a String. If <tt>pretty</tt> is true,
     *  uses a pretty whitespace format, otherwise a compact format.
     *
     * @param element  the element to serialize.
     * @param pretty   if true, use a pretty whitespace format.
     * @return the serialized Element.
     */
    public static String serialize( Element element, boolean pretty ) {
        return serialize( element,pretty ? Format.getPrettyFormat() : Format.getCompactFormat() );
    }

    /**
     *  Serializes the Element to a String. Allows to use a custom <tt>format</tt>.
     *
     * @param element  the element to serialize.
     * @param format   custom <tt>format</tt> used to serialize the Element.
     * @return the serialized Element.
     */
    public static String serialize( Element element, Format format ) {
        XMLOutputter out = new XMLOutputter( format );
        return out.outputString( element );
    }

    /**
     *  Return an Element with an element type name matching the parameter.
     *
     * @param element  the XHTML element type.
     * @return a JDOM2 Element.
     */
    public static Element element( XHTML element ) {
        return element( element, null );
    }

    /**
     *  Return an Element with an element type name matching the parameter,
     *  and optional PCDATA (parsed character data, a String) content.
     *
     * @param element  the XHTML element type.
     * @param content  the optional PCDATA content.
     * @return a JDOM2 Element.
     */
    public static Element element( XHTML element, String content ) {
        Element elt = new Element( element.name() );
        if( content != null ) {
            elt.addContent( content );
        }
        return elt;
    }

    /**
     *  Return an XHTML link with a required 'href' attribute value and optional link (PCDATA) content.
     *
     * @param href     the required 'href' value.
     * @param content  the optional link (PCDATA) content.
     * @return a JDOM2 Element.
     */
    public static Element link( String href, String content ) {
        if( href == null ) {
            throw new IllegalArgumentException("missing 'href' attribute value.");
        }
        return fLink(href,content,null);
    }

    /**
     *  Return an XHTML link target with a required 'id' attribute value.
     *
     * @param id    the required 'id' link target value.
     * @return a JDOM2 Element.
     */
    public static Element target( String id, String content ) {
        if( id == null ) {
            throw new IllegalArgumentException( "missing 'id' attribute value." );
        }
        return fLink( null, content, id );
    }

    /**
     *  Return an XHTML link with an optional 'href' attribute, optional
     *  link content, and optional 'id' link target value.
     *
     * @param href     the optional 'href' value.
     * @param content  the optional link (PCDATA) content.
     * @param id       the optional 'id' link target value.
     * @return a JDOM2 Element.
     */
    private static Element fLink( String href, String content, String id ) {
        Element a = element( XHTML.a );
        if( href != null ) {
            a.setAttribute( XHTML.ATTR_href, href );
        }
        if( content != null ) {
            a.addContent( content );
        }
        if( id != null ) {
            a.setAttribute( XHTML.ATTR_id, id );
        }
        return a;
    }

    /**
     *  Return an XHTML <tt>img</tt> element with an required 'src' attribute
     *  and optional 'alt' alternative text value.
     *
     * @param src      the required 'src' value.
     * @param alt      the optional 'alt' alternative text value.
     * @return a JDOM2 Element.
     */
    public static Element img( String src, String alt ) {
        Element img = element( XHTML.img );
        if( src == null ) {
            throw new IllegalArgumentException( "missing 'src' attribute value." );
        }
        img.setAttribute( XHTML.ATTR_src, src );
        if( alt != null ) {
            img.setAttribute( XHTML.ATTR_alt, alt );
        }
        return img;
    }

    /**
     *  Return an XHTML form <tt>input</tt> element with optional 'type', 'name' and 'value' attributes.
     *
     * @param type   the optional 'type' value.
     * @param name   the optional 'name' value.
     * @param value  the optional 'value' value.
     * @return a JDOM2 Element.
     */
    public static Element input( String type, String name, String value ) {
        Element input = element( XHTML.input );
        if( type != null ) {
            input.setAttribute( XHTML.ATTR_type, type );
        }
        if( name != null ) {
            input.setAttribute( XHTML.ATTR_name, name );
        }
        if( value != null ) {
            input.setAttribute( XHTML.ATTR_value, value );
        }
        return input;
    }

    public static void setClass( Element element, String classValue ) {
        if( classValue == null ) {
            throw new IllegalArgumentException( "missing 'class' attribute value." );
        }
        element.setAttribute( XHTML.ATTR_class, classValue );
    }

}