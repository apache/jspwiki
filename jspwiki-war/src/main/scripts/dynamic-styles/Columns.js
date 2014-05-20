/*
    JSPWiki - a JSP-based WikiWiki clone.

    Licensed to the Apache Software Foundation (ASF) under one
    or more contributor license agreements.  See the NOTICE file
    distributed with this work for additional information
    regarding copyright ownership.  The ASF licenses this file
    to you under the Apache License, Version 2.0 (the
    "License"); fyou may not use this file except in compliance
    with the License.  You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing,
    software distributed under the License is distributed on an
    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
    KIND, either express or implied.  See the License for the
    specific language governing permissions and limitations
    under the License.
*/
/*
Script: Columns
    Format the page content side by side, in columns, like in a newspaper.
    HR elements (in wiki {{----}} markup) separate the columns.
    Column widths are equal and automatically calculated.
    Optionally, you can specify the width in pixel(px) for the columns.

    FSS: use HTML5/CSS3 columns options if available

Arguments:
    width - (optional) column width in pixel(px)

Example:
(start code)
    %%columms-300
        column-text1 ...
        ----
        column-text1 ...
    /%
(end)

DOM Structure
(start code)
    div-columns
        div.col[styles={width:xx%}]
        div.col[styles={width:xx%}]
(end)
*/
function Columns(element, options){

    var args = options.prefix.sliceArgs(element),
        columnCount = element.getElements('hr').length,
        width;

    if( columnCount /*>0*/ ){

        columnCount++;
        width = ( args[0] ) ? args[0]/columnCount+'px' : 100/columnCount+'%';

        element
            .addClass('columns')
            .grab('hr'.slick(),'top') //add one extra group-start-element at the top

            .groupChildren('hr', 'div.col', function(col){ col.setStyle('width',width); });

    }

}
