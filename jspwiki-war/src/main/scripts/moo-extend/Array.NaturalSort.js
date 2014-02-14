/*
 Function: naturalSort
        Sorts the elements of an array, using a more 'natural' algoritm.
        Maintains a cache of the prepared sortable array.

 Example:
        [0, 1, "017", 6, , 21 ].naturalSort();  //[0, 1, 6, "017", 21]

        [[6,"chap 1-14"],["05","chap 1-4"]].naturalSort(1); //[["05","chap 1-4"],[6,"chap 1-14"]]
        rows.naturalSort( 3 );

 */
/*jshint forin:false, noarg:true, noempty:true, undef:true, unused:true, plusplus:false, immed:false, browser:true, mootools:true */

!function(){

    /*
    Function: makeSortable
        Parse the column and guess its data-type.
        Then convert all values according to that data-type.
        Cache the sortable values in rows[0-n].cache.
        Empty rows will sort based on the title attribute of the cells.

    Supported data-types:
        numeric - numeric value, with . as decimal separator
        date - dates as supported by javascript Date.parse
          See https://developer.mozilla.org/en/Core_JavaScript_1.5_Reference/Global_Objects/Date/parse
        ip4 - ip addresses (like 169.169.0.1)
        euro - currency values (like £10.4, $50, €0.5)
        kmgt - storage values (like 2 MB, 4GB, 1.2kb, 8Tb)

    Arguments:
        rows - array of rows each pointing to a DOM tr element
            rows[i].data caches the converted data.
        column - index (0..n) of the processed column

    Returns:
        comparison function which can be used to sort the table
    */
    function makeSortable(thisArray, column){

        var num=[], dmy=[], kmgt=[], nat=[], val, i, len = thisArray.length, isNode,

            //split string in sequences of digits
            reNAT = /([-+]?\d+)|(\D+)/g,

            KMGTre = /(:?[\d.,]+)\s*([kmgt])b/,    //eg 2 MB, 4GB, 1.2kb, 8Tb
            KMGTmul = { k:1, m:1e3, g:1e6, t:1e9 },
            KMGTparse = function( val ){
                return KMGTre.test( val.toLowerCase() ) ?
                    val.toFloat() * KMGTmul[ RegExp.$2 ] : NaN;
            };

        for( i=0; i<len; i++ ){

            //1. Retrieve the value to be sorted: native js value, or dom elements

            val = thisArray[i];
            isNode = val && val.nodeType;

            //if 'column' => retrieve the nth DOM-element or the nth Array-item
            if( !isNaN(column) ) val = ( isNode ? val.getChildren() : val )[column];

            //retrieve the value and convert to string
            val = (''+(isNode ? val.get('text') || val.get('title') : val)).trim();

            //2. Convert and store in type specific arrays (num,dmy,kmgt,nat)

            //CHECKME: some corner cases: numbers with leading zero's, confusing date string
            if( /(?:^0\d+)|(^[^+-\d]+\d+$)/.test(val) ){ num=dmy=0; }

            if( num && isNaN( num[i] = +val ) ) num=0;

            if( nat && !( nat[i] = val.match(reNAT) ) ) nat=0;

            //Only strings with non-numeric values
            if( dmy && ( num || isNaN( dmy[i] = Date.parse(val) ) ) ) dmy=0;

            if( kmgt && isNaN( kmgt[i] = KMGTparse(val) ) ) kmgt=0;

        }

        console.log("[",kmgt?"kmgt":dmy?"dmy":num?"num":nat?"nat":'no conversion',"] ");
        //console.log(nat);
        //console.log(kmgt||dmy||num||nat||thisArray);

        return kmgt || dmy || num || nat || thisArray;

    }

    /*
    Function: naturalCmp
        Comparison function for sorting "natural sortable" arrays.
        The entries of sortable arrays consists of tupples:
        ( .[1] is the sortable value, .[0] is the original value )

        The sortable value is either a scalar or an array.
    */
    function naturalCmp(a,b){

        var aa, bb, i=0, t;

        // retrieve the sortable values: scalars or tokenized arrays
        a = a[1]; b = b[1];

        // scalars, always same types - integer, float, date, string
        if( typeof a !='object' ) return (a<b) ? -1 : (a>b) ? 1 : 0;
        //if( !a.length ) return a.localeCompare(b);

        while( (aa = a[i]) ){

            if( !( bb = b[i++] ) ) return 1; //fixme

            t = aa - bb;       //auto-conversion to numbers, if possible
            if( t ) return t;  //otherwise fall-through to string comparison

            if( aa !== bb ) return (aa > bb) ? 1 : -1;
            //if( aa !== bb ) return aa.localeCompare(bb);

        }
        return b[i] ? -1 : 0;
    }


    Array.implement('naturalSort',function(column, force){

        var thisArray = this, sortable, i, len = thisArray.length,
            cache = 'cache';

console.log('naturalSort',column,force)
        //1. read sortable cache or make a new sortable array
        if( isNaN(column) ){    // 1D array : [ .. ]

            sortable = thisArray[cache] || [];

            if( column/*==force*/ || !sortable.length/*==0*/ ){

                sortable = thisArray[cache] = makeSortable(thisArray);

            }

        } else {    // 2D array : [[..],[..],..]

            sortable = thisArray[0][cache] || [];

            if( !sortable.length ) for(i=0; i<len; i++) thisArray[i][cache] = []; //init row caches

            if( force || (sortable[column]==undefined) ){

                sortable = makeSortable(thisArray, column);
                for(i=0; i<len; i++) thisArray[i][cache][column] = sortable[i]; //cache sortable values

            } else {

                for(i=0; i<len; i++) sortable[i]=thisArray[i][cache][column];  //retrieve cached column

             }

        }

console.log(this.cache);
        //2. Do the actual sorting
        for( i=0; i<len; i++) sortable[i] = [ thisArray[i], sortable[i] ];
        sortable.sort( naturalCmp );
        for( i=0; i<len; i++) thisArray[i] = sortable[i][0];

        return thisArray;

    });

}();