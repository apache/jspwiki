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

/**
 *  Provides constants for the Extensible HyperText Markup Language (XHTML) 1.0.
 *
 * @since     2.10
 */
public enum XHTML 
{
    // XHTML element type names ...........................................

    a, abbr, acronym, address, applet, area,
    b, base, basefont, bdo, big, blockquote, body, br, button,
    caption, center, cite, code, col, colgroup,
    dd, del, dfn, dir, div, dl, dt,
    em,
    fieldset, font, form, frame, frameset,
    h1, h2, h3, h4, h5, h6, head, hr, html,
    i, iframe, img, input, ins, isindex,
    kbd,
    label,
    legend, li, link,
    map, menu, meta,
    noframes, noscript,
    object, ol, optgroup, option,
    p, param, pre,
    q,
    s, samp, script, select, small, span, strike, strong, style, sub, sup,
    table, tbody, td, textarea, tfoot, th, thead, title, tr, tt,
    u, ul,
    var;


    /** 
     *  Returns the element type name (AKA generic identifier).
     *  This is a convenience method that returns the same value 
     *  as <tt>XHTML.&lt;GI&gt;>.name();</tt>.
     */
    public String getName()
    {
        return this.name();
    }

    
    // XML Public and Namespace identifiers for XHTML .........................

    /** XML Namespace URI for the Extensible HyperText Markup Language (XHTML). */
    public static final String XMLNS_xhtml = "http://www.w3.org/1999/xhtml";

    /** A String containing the Formal Public Identifier (FPI) for the XHTML 1.0 Strict DTD. */
    public static final String STRICT_DTD_PubId = "-//W3C//DTD XHTML 1.0 Strict//EN";
    /** A String containing a system identifier for the XHTML 1.0 Strict DTD. */
    public static String STRICT_DTD_SysId = "xhtml1-strict.dtd";

    /** A String containing the Formal Public Identifier (FPI) for the XHTML 1.0 Transitional DTD. */
    public static final String TRANSITIONAL_DTD_PubId = "-//W3C//DTD XHTML 1.0 Transitional//EN";
    /** A String containing a system identifier for the XHTML 1.0 Transitional DTD. */
    public static String TRANSITIONAL_DTD_SysId = "xhtml1-transitional.dtd";

    /** A String containing the Formal Public Identifier (FPI) for the XHTML 1.0 Frameset DTD. */
    public static final String FRAMESET_DTD_PubId = "-//W3C//DTD XHTML 1.0 Frameset//EN";
    /** A String containing a system identifier for the XHTML 1.0 Frameset DTD. */
    public static String FRAMESET_DTD_SysId = "xhtml1-frameset.dtd";

    // CSS strings ........................................................

    /** An identifier for the CSS stylesheet notation using its W3C home page URI. */
    public static final String CSS_style  = "http://www.w3.org/Style/CSS/";


    // XHTML common attribute names .......................................

    /** String containing the XHTML 'id' attribute name (i.e., 'id'). */
    public static final String ATTR_id            = "id";

    /** String containing the XHTML 'class' attribute name (i.e., 'class'). */
    public static final String ATTR_class         = "class";

    /** String containing the XHTML 'name' attribute name (i.e., 'name'). */
    public static final String ATTR_name          = "name";

    /** String containing the XHTML 'type' attribute name (i.e., 'type'). */
    public static final String ATTR_type          = "type";

    /** String containing the XHTML 'value' attribute name (i.e., 'value'). */
    public static final String ATTR_value         = "value";

    /** String containing the XHTML 'href' attribute name (i.e., 'href'). */
    public static final String ATTR_href          = "href";

    /** String containing the XHTML 'title' attribute name (i.e., 'title'). */
    public static final String ATTR_title         = "title";

    /** String containing the XHTML 'width' attribute name (i.e., 'width'). */
    public static final String ATTR_width         = "width";

    /** String containing the XHTML 'height' attribute name (i.e., 'height'). */
    public static final String ATTR_height        = "height";

    /** String containing the XHTML 'border' attribute name (i.e., 'border'). */
    public static final String ATTR_border        = "border";

    /** String containing the XHTML 'colspan' attribute name (i.e., 'colspan'). */
    public static final String ATTR_colspan       = "colspan";

    /** String containing the XHTML 'src' attribute name (i.e., 'src'). */
    public static final String ATTR_src           = "src";

    /** String containing the XHTML 'alt' attribute name (i.e., 'alt'). */
    public static final String ATTR_alt           = "alt";

    // okay, maybe not so common

    /** String containing the XHTML 'bgcolor' attribute name (i.e., 'bgcolor'). */
    public static final String ATTR_bgcolor       = "bgcolor";

    /** String containing the XHTML 'checked' attribute name (i.e., 'checked'). */
    public static final String ATTR_checked       = "checked";
    
    /** String containing the XHTML 'cols' attribute name (i.e., 'cols'). */
    public static final String ATTR_cols          = "cols";
    
    /** String containing the XHTML 'content' attribute name (i.e., 'content'). */
    public static final String ATTR_content       = "content";

    /** String containing the XHTML 'http-equiv' attribute name (i.e., 'http-equiv'). */
    public static final String ATTR_httpEquiv     = "http-equiv";

    /** String containing the XHTML 'scheme' attribute name (i.e., 'scheme'). */
    public static final String ATTR_scheme        = "scheme";

    /** String containing the XHTML 'rel' attribute name (i.e., 'rel'). */
    public static final String ATTR_rel           = "rel";

    /** String containing the XHTML 'rows' attribute name (i.e., 'rows'). */
    public static final String ATTR_rows          = "rows";
    
    /** String containing the XHTML 'selected' attribute name (i.e., 'selected'). */
    public static final String ATTR_selected      = "selected";
    
    /** String containing the XHTML 'size' attribute name (i.e., 'size'). */
    public static final String ATTR_size          = "size";
    
    /** String containing the XHTML 'style' attribute name (i.e., 'style'). */
    public static final String ATTR_style         = "style";

    /** String containing the XHTML 'align' attribute name (i.e., 'align'). */
    public static final String ATTR_align         = "align";

    /** String containing the XHTML 'cellpadding' attribute name (i.e., 'cellpadding'). */
    public static final String ATTR_cellpadding   = "cellpadding";
    
    /** String containing the XHTML 'cellspacing' attribute name (i.e., 'cellspacing'). */
    public static final String ATTR_cellspacing   = "cellspacing";

    // XHTML character entities ...........................................

    /**
     *  Returns a String containing the named character entity corresponding to
     *  the character number <tt>num</tt> for the range 160-255. Throws an 
     *  ArrayOutOfBoundsException if beyond the prescribed range.
     */
    public static String getNamedCharacterEntity( int num )
    {
        return CHARACTER_ENTITIES[num-160];
    }

    /* Conversion table for the XHTML upper ASCII character entities (character numbers 160-255). */
    private static final String[] CHARACTER_ENTITIES = {
        "nbsp" /* 160 */,   "iexcl" /* 161 */,  "cent" /* 162 */,   "pound" /* 163 */,
        "curren" /* 164 */, "yen" /* 165 */,    "brvbar" /* 166 */, "sect" /* 167 */,
        "uml" /* 168 */,    "copy" /* 169 */,   "ordf" /* 170 */,   "laquo" /* 171 */,
        "not" /* 172 */,    "shy" /* 173 */,    "reg" /* 174 */,    "macr" /* 175 */,
        "deg" /* 176 */,    "plusmn" /* 177 */, "sup2" /* 178 */,   "sup3" /* 179 */,
        "acute" /* 180 */,  "micro" /* 181 */,  "para" /* 182 */,   "middot" /* 183 */,
        "cedil" /* 184 */,  "sup1" /* 185 */,   "ordm" /* 186 */,   "raquo" /* 187 */,
        "frac14" /* 188 */, "frac12" /* 189 */, "frac34" /* 190 */, "iquest" /* 191 */,
        "Agrave" /* 192 */, "Aacute" /* 193 */, "Acirc" /* 194 */,  "Atilde" /* 195 */,
        "Auml" /* 196 */,   "Aring" /* 197 */,  "AElig" /* 198 */,  "Ccedil" /* 199 */,
        "Egrave" /* 200 */, "Eacute" /* 201 */, "Ecirc" /* 202 */,  "Euml" /* 203 */,
        "Igrave" /* 204 */, "Iacute" /* 205 */, "Icirc" /* 206 */,  "Iuml" /* 207 */,
        "ETH" /* 208 */,    "Ntilde" /* 209 */, "Ograve" /* 210 */, "Oacute" /* 211 */,
        "Ocirc" /* 212 */,  "Otilde" /* 213 */, "Ouml" /* 214 */,   "times" /* 215 */,
        "Oslash" /* 216 */, "Ugrave" /* 217 */, "Uacute" /* 218 */, "Ucirc" /* 219 */,
        "Uuml" /* 220 */,   "Yacute" /* 221 */, "THORN" /* 222 */,  "szlig" /* 223 */,
        "agrave" /* 224 */, "aacute" /* 225 */, "acirc" /* 226 */,  "atilde" /* 227 */,
        "auml" /* 228 */,   "aring" /* 229 */,  "aelig" /* 230 */,  "ccedil" /* 231 */,
        "egrave" /* 232 */, "eacute" /* 233 */, "ecirc" /* 234 */,  "euml" /* 235 */,
        "igrave" /* 236 */, "iacute" /* 237 */, "icirc" /* 238 */,  "iuml" /* 239 */,
        "eth" /* 240 */,    "ntilde" /* 241 */, "ograve" /* 242 */, "oacute" /* 243 */,
        "ocirc" /* 244 */,  "otilde" /* 245 */, "ouml" /* 246 */,   "divide" /* 247 */,
        "oslash" /* 248 */, "ugrave" /* 249 */, "uacute" /* 250 */, "ucirc" /* 251 */,
        "uuml" /* 252 */,   "yacute" /* 253 */, "thorn" /* 254 */,  "yuml" /* 255 */ };

} // end org.apache.wiki.util.XHTML
