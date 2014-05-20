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
Class: TableX.Filter
    Allows to filter html tables based on regexp enabled filter search box.
    Filtering happens both on column and row basis.

Credits:
    Filters inspired by http://www.codeproject.com/jscript/filter.asp  and
    jquery.filterTable by Sunny Walker http://sunnywalker.github.io/jQuery.FilterTable/

*/
TableX.Filter = new Class({

    Implements: Options,

    options :{
        minSize: 5,  //don't show the filter on tables with less than this number of rows
        shortcut: 'a.btn.btn-link[text="{0}"]', //template for shortcut filter-strings
        list: [], //['99','test'],  //TODO list of shortcuts to quickly filter the table
        hint: 'filter this table',  //HTML5 placeholder text for the filter field
        highlight: 'highlight'  //class applied to cells containing the filter term
    },

    initialize: function(table, options){

        options = this.setOptions(options).options;
        
        var self = this,
            items = [],
            minRows = options.minRows,
            filter = self.filter.bind(self);


        self.table = table = new TableX(table, {minSize:options.minSize});

        if( table && table.table){

            options.list.each(function(item){
                items.push( options.shortcut.xsubs(item),{
                    events:{click : self.shortcut.pass(item,self) }
                });
            });

            ['div.form-group.filter-input',[
                'span.icon-filter',
                'input.form-control[type=search][placeholder="'+options.hint+'"]',{
                    attach: [ self, 'input' ],
                    events: {
                        keyup: filter,    //'keyup:throttle': filter,
                        click: filter
                    }
                }],
                items
            ].slick().inject(table.table,'before');

        }
    },

    shortcut: function(value){
        this.input.set('value', value).fireEvent('click').focus();
        return false;
    },

    filter: function(){

        var self = this,
            visible = 'visible',
            highlight = self.options.highlight,

            rows = self.table.rows,
            cells = self.table.cells.removeClass(highlight),

            query = self.input.value,
            queryRE;


        try { queryRE = RegExp( query/*.escapeRegExp()*/, 'i'); } catch(e){}

        if( query == '' || !queryRE ){

            rows.show().addClass(visible);

        } else {

            rows.hide().removeClass(visible); //hide all

            cells.filter( function(el){

                return queryRE.test( $(el).get('text') );

            }).addClass(highlight).getParent(/*tr*/).show().addClass(visible);

        }

        self.table.refresh();
    }

});
