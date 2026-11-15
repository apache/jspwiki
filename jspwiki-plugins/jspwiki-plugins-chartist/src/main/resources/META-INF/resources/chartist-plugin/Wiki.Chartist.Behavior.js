/*
 * Copyright 2025 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
var ChartistChart = new Class({

    Implements: Options,

    options: {
        container: "chartist-line" //classname of container
    },

    initialize: function(el, options){


        var self = this,
            args = el.className,
            bars, data, i, len, table, clazz;

        self.setOptions(options);

        clazz = this.options.container;

        this.render();
    },

    getArguments: function( args ){

        var options = this.options;
        return options;
    },


    /*
    Function: render
        Render a graphBar and add it before or inside the element.
    Arguments:
        el - element
        val - converted value in range 0-100,
        percent - position of the graphBar in the group, converted to a %
    DOM Structure of a graphbar
    (start code)
        span.graphBar-Group[style="width=..px"]
            span.graphBar[style="width=..%,background-color=.."]
            span.graphBar[style="width=..%,background-color=.."] //only for progress bars
    (end)
    */
    render: function(el, val, percent){
        console.debug("looking for charts to build");
        var css_selector = '[class^=chartist]';
        const matches = document.querySelectorAll(css_selector);
        console.debug("found " + matches.length + " charts to build");
        for (var i=0; i < matches.length; i++) {
            this.chartist_behavior(matches[i]);
        };


    },
    chartist_behavior: function (element) {
        var type = element.className.split('-')[1] || 'line', // line or bar or pie
            //var type = chartistClass.sliceArgs(element)[0] || "line",  // line or bar or pie
            options = this.grabOptions(
                element,
                'span.chartist-options'
            ), //default display:none
            data,
            el;

        type = type.capitalize();

        if (type.match(/Line|Bar|Pie/)) {
            console.info("chartifying element ");
            console.debug(element);


            var pageContent = element.querySelector('.wikitable');
            if (null===pageContent) {
                pageContent = element.parentElement.parentElement;
                //iterate until we find 'element', then get the next table element
                var x=0;
                var found=false;
                for (x=0; x < pageContent.children.length; x++) {
                    var p = pageContent.children[x];
                    //this should be a "p" element.
                    for (var j=0; j < p.children.length; j++) {
                        if (p.children[j] === element) {
                            found = true;
                            break;
                        }
                    }
                    if (found) break;
                }

                if (found) {
                    element = pageContent.children[x+1];
                    //this should be the next "p" containing the table.
                } else {
                    console.warn("failed to locate the table for " + element);
                    return;
                }
            } else {
                element = pageContent;
            }

            data = this.getTableData(element);
            var table = element;
            if (data) {
                console.debug("got table data " + type);
                console.debug(data);
                el = ['div', ['div.ct-chart.ct-golden-section']]
                    .slick()
                    .inject(table, 'after');

                table.addClass('chartist-table'); //default display:none;

                if (type == 'Pie') {
                    data.series = data.series[0];
                }
                //Chartist(el.getFirst(), data, { type: type },
                //Wiki.evalOptions(options, data.labels, data.series))

                new Chartist[type + "Chart"](
                    el.getFirst(),
                    data,
                    this.evalOptions(options, data.labels, data.series)
                );
            } else {
                console.warn("no data returned for chart, skipping");
            }

        } else {
            console.warn("skipping element as the type " + type + " doesn't match anything i currently process");
        }
    },
    /*
Function: grabOptions
    Read the chartist options, and encapsulate it in a hidden container dom element
*/
    grabOptions: function (element, container) {
        var el,
            fragment = new DocumentFragment();

        while ((el = element.firstChild) && el.nodeType == 3) {
            fragment.appendChild(el);
        }

        fragment = fragment.textContent.trim();

        if (fragment != '') {
            container.slick({ text: fragment }).inject(element, 'top');
        }
        return fragment;
    },

    /*
Function: evalOptions
    Validate and parse the options string, into a regular javascript object
*/
    evalOptions: function (options, labels, series) {
        if (options != '') {
            try {
                return Function(
                    'labels',
                    'series',
                    'return ' + options
                )(labels, series); // jshint ignore:line
            } catch (err) {
                console.log('Options eval err', err, options);
                return null;
            }
        }
    },

    /*
Function: getTableData
    Parse regular html table, and collect the LABELS and SERIES data-sets.
*/
    getTableData: function (table) {
        var rows = table.rows,
            tlen = rows.length,
            i,
            j,
            row,
            rlen,
            labels = undefined,
            series = [];

        for (i = 0; i < tlen; i++) {
            row = Array.from(rows[i].cells);
            rlen = row.length;

            if (row[0].tagName.test(/TH/i)) {
                //get LABELS
                labels = [];
                for (j = 0; j < rlen; j++) {
                    labels[j] = row[j].innerHTML;
                }
            } else {
                //get SERIES ; convert to numbers
                for (j = 0; j < rlen; j++) {
                    row[j] = +row[j].textContent;
                }
                series.push(row);
            }
        }
        console.debug("chart data returned was " + series);
        return series[0] ? { labels: labels, series: series } : null;
    }
});

Wiki.add( ".chartist-line", function( element ){
	new ChartistChart( ( element ), {
		container: element
	});
})
Wiki.add( ".chartist-bar", function( element ){
	new ChartistChart( ( element ), {
		container: element
	});
})
Wiki.add( ".chartist-pie", function( element ){
	new ChartistChart( ( element ), {
		container: element
	});
})