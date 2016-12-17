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
Class: TableX
    Base class, with reusable table support functions.
    Used by TableX.Sort, TableX.Zebra, ...


Usage:
    > var t = new TableX(table-element);
    Creates maximum one TableX instance per table DOM element.

    > t.addEvent('onRefresh', console.log('table got refreshed.'));
    > t.refresh(new-rows-array);

*/
var TableX = new Class({

    Implements: [Options,Events],

    initialize: function(table, options){

        var self = table.TableX,  //max one TableXtend instance per table DOM element
            minSize = ( options||{} ).minSize||0;

        if( !self ){

            if( !table.match('table') ||
                ( ( minSize>0 )&&( table.rows.length < minSize ) ) ){
                console.log("Warning TableX size: ",table," minSize:",minSize);
                return false;
            }

            table.TableX = self = this;
            self.table = table;
            self.thead = $(table.rows[0]).getChildren('th');
            self.rows  = $$(Array.slice(table.rows, self.thead.length>0 ? 1 : 0));
            self.cells = table.getElements('td'); //fixme: check for nested tables

        }
        return self && self.setOptions(options);  //set additional options
    },

    /*
    Function:refresh
        Put array of table rows back into the table
    */
    refresh: function( rows ){

        //console.log(rows);

        if( rows ){

            var frag = document.createDocumentFragment();
            rows.each( function(r){ frag.appendChild(r); });
            this.table.tBodies[0].appendChild(frag);

        }

        this.fireEvent('refresh');

    },

    /*
    Function: filter
        Fetch a row or column from a table, based on a field-name
        * check first-row to match field-name: return array with col values
        * check first-column to match field-name: return array with row values
        * ?? insert SPANs as place-holder of the missing gBars

    */
    filter: function( fieldName ){

        var rows = this.table.rows,
            tlen = rows.length, col, l, r, i,
            result = [];

        if( tlen > 1 ){ // first check for COLUMN based table

            r = rows[0]; //header row

            for( col=0, l=r.cells.length; col<l; col++ ){

                if( $(r.cells[col]).get('text').trim() == fieldName ){

                    //take this COLUMN
                    for( i=1; i < tlen; i++){
                        //result.push( new Element('span').wraps(table.rows[i].cells[col]) );
                        result[result.length] = rows[i].cells[col];
                    }
                    return result;
                }
            }
        }

        for( i=0; i < tlen; i++ ){  // finally, check for ROW based table

            r = rows[i];

            if( $(r.cells[0]).get('text').trim() == fieldName ){
                //take this ROW
                return  Array.slice(r.cells,1);
            }
        }

        return false;
    }

});
