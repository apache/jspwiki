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
Class: TableX.Zebra
    Simple class to add odd/even coloring to tables.
    Default colouring via css class "".odd""

    CHECKME: use BOOTSTRAP class .striped

Usage:
    > new TableX.Zebra( table-element, {colors:["eee","fff"]});
    > new TableX.Zebra( table-element, {colors:["red"]});

*/
TableX.Zebra = function(table, options){

    function stripe(){

        this.rows.filter( function(el){
            return el.getStyle("display") != "none";
        }).each( function(row, j){

            j &= 1; //0,1,0,1...

            if( hasColors ){

                row.setStyle("background-color", colors[j] || "");

            } else {

                row.ifClass(j, "odd");

            }

        });
    }

    var colors = options.colors,
        hasColors = colors[0] && (colors[0] != "table");

    if ( hasColors ){ colors = colors.map( function(c){ return new Color(c); }); }

    //console.log("ZEBRA ",hasColors, options.colors, colors[0], colors[1]);
    stripe.call( new TableX(table, { onRefresh: stripe }) );

};
