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
const { task, src, dest, watch, series, parallel } = require('gulp');
const less = require('gulp-less');
const eslint = require('gulp-eslint');
const uglify = require('gulp-uglify');
const concat = require('gulp-concat');
const jasmine = require('gulp-jasmine');
const rename = require('gulp-rename'); //check
//const useref = require('gulp-useref');

/**
 * Configuration
 */

const source = './src/main';
const target = './target/generated-sources/gulp';
const scripts = `${source}/scripts`;
const styles = `${source}/styles`;
const wysiwygStyles = `${scripts}/wiki_wysiwyg/Assets/MooEditable`;
const jasmineScripts = './src/test/scripts';

const lib = [
    `${scripts}/lib/mootools-core-1.6.0.js`,
    `${scripts}/lib/mootools-more-1.6.0.js`,
    `${scripts}/lib/prettify4mar13.js`
];
const mooextend = [
    `${scripts}/moo-extend/Function.Extend.js`,
    `${scripts}/moo-extend/Behavior.js`,
    `${scripts}/moo-extend/String.Extend.js`,
    //`${scripts}/moo-extend/Date.Extend.js`,
    `${scripts}/moo-extend/Array.Extend.js`,
    `${scripts}/moo-extend/Array.NaturalSort.js`,
    `${scripts}/moo-extend/Element.Extend.js`,
    `${scripts}/moo-extend/Color.js`,
    `${scripts}/moo-extend/HighlightQuery.js`,
    `${scripts}/moo-extend/Accesskey.js`,
    `${scripts}/moo-extend/Tips.js`,
    `${scripts}/moo-extend/Request.File.js`,
    `${scripts}/moo-extend/Form.MultipleFile.js`,
    `${scripts}/moo-extend/Form.File.js`
];
const behaviors = [
    `${scripts}/behaviors/AddCSS.js`,
    `${scripts}/behaviors/Collapsible.js`,
    `${scripts}/behaviors/CommentBox.js`,
    `${scripts}/behaviors/Columns.js`,
    `${scripts}/behaviors/GraphBar.js`,
    `${scripts}/behaviors/Element.Reflect.js`,
    `${scripts}/behaviors/Flip.js`,
    `${scripts}/behaviors/Magnify.js`,
    `${scripts}/behaviors/TableX.js`,
    `${scripts}/behaviors/TableX.Sort.js`,
    `${scripts}/behaviors/TableX.Filter.js`,
    `${scripts}/behaviors/TableX.Zebra.js`,
    `${scripts}/behaviors/Tabs.js`,
    `${scripts}/behaviors/Accordion.js`,
    `${scripts}/behaviors/Viewer.js`,
    `${scripts}/behaviors/Viewer.Slimbox.js`,
    `${scripts}/behaviors/Viewer.Carousel.js`
];
const haddock = [
    ...lib,
    ...mooextend,
    ...behaviors,
    `${scripts}/wiki/Wiki.js`,
    `${scripts}/wiki/Search.js`,
    `${scripts}/wiki/Recents.js`,
    `${scripts}/wiki/Findpages.js`,
    `${scripts}/wiki/Category.js`,
    `${scripts}/wiki/Wiki.Behaviors.js`,
    `${scripts}/wiki/Prefs.js`
];
const haddock_wysiwyg = [
    `${scripts}/wiki-wysiwyg/Source/MooEditable/MooEditable.js`,
    `${scripts}/wiki-wysiwyg/Source/MooEditable/MooEditable.UI.MenuList.js`,
    `${scripts}/wiki-wysiwyg/Source/MooEditable/MooEditable.Extras.js`
];
const haddock_edit = [
    `${scripts}/moo-extend/Date.Extend.js`,
    `${scripts}/moo-extend/Array.Extend.HSV.js`,
    `${scripts}/dialog/Dialog.js`,
    `${scripts}/dialog/Dialog.Buttons.js`,
    `${scripts}/dialog/Dialog.Color.js`,
    `${scripts}/dialog/Dialog.Selection.js`,
    `${scripts}/dialog/Dialog.Font.js`,
    `${scripts}/dialog/Dialog.Chars.js`,
    `${scripts}/dialog/Dialog.Find.js`,
    `${scripts}/moo-extend/Textarea.js`,
    `${scripts}/wiki-edit/Html2Wiki.js`,
    `${scripts}/wiki-edit/Undoable.js`,
    `${scripts}/wiki-edit/Snipe.js`,
    `${scripts}/wiki-edit/Snipe.Snips.js`,
    `${scripts}/wiki-edit/Snipe.Commands.js`,
    `${scripts}/wiki-edit/Snipe.Sections.js`,
    `${scripts}/wiki-edit/Wiki.Snips.js`,
    `${scripts}/wiki-edit/Wiki.Edit.js`
];
const less = [];

/**
 * GULP pipelines
 */
task('init', () => {
    console.log(haddock);
    console.log(haddock_wysiwyg);
    console.log(haddock_edit);
});

task('concatjs', () => {
    return src(haddock)
        .pipe(concat('haddock.js'))
        .pipe(dest(`${target}/scripts`));
});

task('eslint', () => {
    return src([`${scripts}/*/*.js`, `!${scripts}/lib/**`])
        .pipe(eslint())
        .pipe(eslint.format()) //outputs the lint results to the console
        .pipe(eslint.failAfterError());
});

task('jasmine', () => {
    return src(`${jasmineScripts}/index.js`).pipe(
        jasmine({
            verbose: true,
            includeStackTrace: true
        })
    );
});

task('minifyjs', () => {
    /*
    let u = uglify();
    u.on('error', function(error) {
        console.error(error);
        u.end();
    });
    */
    return src(`${target}/scripts/*.js`)
        .pipe(uglify)
        .pipe(rename({ suffix: '.min' }))
        .pipe(dest(`${target}/scripts`));
});

task('styles', () => {
    src(`${styles}/haddock/default/build.less`)
        .pipe(less('haddock.css'))
        //.pipe(minifyCSS())
        .pipe(dest(`${target}/templates/default`));

    src([
        `${wysiwygStyles}/MooEditable.css`,
        `${wysiwygStyles}/MooEditable.Extras.css`
    ])
        .pipe(less('haddock_wysiwyg.css'))
        .pipe(minifyCSS())
        .pipe(dest(`${target}/templates/default`));
    //haddock_wysiwyg.css
    //<css>/scripts/wiki-wysiwyg/Assets/MooEditable/MooEditable.css</css>
    //<css>/scripts/wiki-wysiwyg/Assets/MooEditable/MooEditable.Extras.css</css>
    //haddock-dark.css
});

task('watch', () => {
    watch([`${scripts}/**.js`], series('eslint', 'concatjs', 'minifyjs'));
    watch([`${styles}/**.less`], series('styles'));
});

task(
    'default',
    parallel(
        'styles',
        series('eslint', 'concatjs', 'minifyjs' /*, "jasmine" */)
    )
);

/**
 *
 * Supporting functions
 *
 */
