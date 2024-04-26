<!--
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
-->

**2023-12-04  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.2-git-11_

* Call `ServletRequest#setCharacterEncoding` prior to calling `URLConstructor#parsePageFromURL` (related to error noted at [#322](https://github.com/apache/jspwiki/pull/322)), in order to ensure the proper encoding is set.

* Dependency updates
    * Lucene to 9.9.0
    * Mockito to 5.8.0 (closes [#325](https://github.com/apache/jspwiki/pull/325), thanks to dependabot)
    * Maven plugins: javadoc to 3.6.3

**2023-12-02  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.2-git-10_

* Use `Engine#getContentEncoding` to call `URLConstructor#parsePageFromURL` (related to error noted at [#322](https://github.com/apache/jspwiki/pull/322)). The latter also url decodes the returned page name.

* Introduced [`CustomWikiEventListener`](https://jspwiki-wiki.apache.org/Wiki.jsp?page=HowToWriteACustomWikiEventListener) as an easy way to register 3rd party `WikiEventListener`s.

* Dependency updates
    * commons-lang3 to 3.14.0
    * Maven plugins: cargo to 1.10.11 (closes [#324](https://github.com/apache/jspwiki/pull/324), thanks to dependabot)

**2023-11-25  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.2-git-09_

* Make `URLConstructor#parsePageFromURL` work with default URL style (closes [#322](https://github.com/apache/jspwiki/pull/322), thanks to Ulf Dittmer)

* Update `Viewer.js` so that Vimeo uses html5 player instead of the flash based one (closes [#321](https://github.com/apache/jspwiki/pull/321), thanks to Ulf Dittmer)

* Dependency updates
    * Mockito to 5.7.0 (closes [#319](https://github.com/apache/jspwiki/pull/319), thanks to dependabot)
    * Maven plugins: project-info-reports-plugin to 3.5.0, cargo to 1.10.10 (closes [#320](https://github.com/apache/jspwiki/pull/320), thanks to dependabot)

**2023-11-19  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.2-git-08_

* `AttachmentServlet` now respects `jspwiki.attachment.forceDownload` pattern and will refuse to inline content matching those extensions

* Added `.svg` to default `jspwiki.attachment.forceDownload` patterns

* [JSPWIKI-1184](https://issues.apache.org/jira/browse/JSPWIKI-1184) - Image missing and German text typo
  Fixed as suggested by Ulf Dittmer - thanks! 

* Dependency updates
    * Apache Parent to 31
    * Commons IO to 2.15.0
    * Commons Text to 1.11.0
    * Jamm to 0.4.0 (closes [#296](https://github.com/apache/jspwiki/pull/296)), thanks to dependabot
    * Jetty to 9.4.53.v20231009 (closes [#313](https://github.com/apache/jspwiki/pull/313)), thanks to dependabot
    * JUnit to 5.10.1
    * Tika to 2.9.1
    * Tomcat to 9.0.83
    * Maven plugins: clean to 3.3.2, dependency to 3.6.1, javadoc to 3.6.2, jxr to 3.3.1, surefire to 3.2.2, sonar to 3.10.0.2594 (closes [#308](https://github.com/apache/jspwiki/pull/308), thanks to dependabot)

**2023-10-12  Arturo Bernal (abernal AT apache DOT org)**

* _2.12.2-git-07_

Replaced 'size() == 0' with 'isEmpty()'

**2023-10-08  Arturo Bernal (abernal AT apache DOT org)**

* _2.12.2-git-06_

* [JSPWIKI-925](https://issues.apache.org/jira/browse/JSPWIKI-925) - Missing i18n resources

**2023-10-02  Arturo Bernal (abernal AT apache DOT org)**

* _2.12.2-git-05_

* [JSPWIKI-1056](https://issues.apache.org/jira/browse/JSPWIKI-1056) - URL in registration mail is relative while it should be absolute.


**2023-10-02  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.2-git-04_

* Dependency updates
    * AWS Java SDK Kendra to 1.12.565
    * Commons IO to 2.14.0
    * Lucene to 9.8.0
    * Selenide to 6.19.0
    * Tomcat to 9.0.80

**2023-10-02  Arturo Bernal (abernal AT apache DOT org)**

* _2.12.2-git-03_

* [JSPWIKI-1181](https://issues.apache.org/jira/browse/JSPWIKI-1181) - Search popup does not handle attachments correctly.

**2023-09-09  Dirk Frederickx (brushed AT apache DOT org)**

* _2.12.2-git-02_

* [JSPWIKI-1167](https://issues.apache.org/jira/browse/JSPWIKI-1167) - prettify: line numbering is wrong with longer lines
    Prettified code lines should not wrap around, to avoid mismatch with line numbering.
    
**2023-09-09  Dirk Frederickx (brushed AT apache DOT org)**

* _2.12.2-git-01_

* [JSPWIKI-1165](https://issues.apache.org/jira/browse/JSPWIKI-1165) - long text in monospace font inside {{}} is shown without scroll bar
    Inline preformatted text will wrap as necessary, and stay within the width of the line boxes. 

**2023-09-06  Arturo Bernal (abernal AT apache DOT org)**

* [JSPWIKI-778](https://issues.apache.org/jira/browse/JSPWIKI-778) - JSPWiki missing some translations in Finnish

* No version bump

**2023-07-29  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.1-git-02_

* Merged a lot of PRs from Arturo Bernal - thanks!
    * [#272](https://github.com/apache/jspwiki/pull/272): Java 11 improvements
    * [#282](https://github.com/apache/jspwiki/pull/282): Set `jspwiki.workDir` default to servlet context temp directory (fixes [JSPWIKI-1172](https://issues.apache.org/jira/browse/JSPWIKI-1172))
    * [#290](https://github.com/apache/jspwiki/pull/290): Constant charset String literal can be replaced with `StandardCharsets.UTF_8`
    * [#291](https://github.com/apache/jspwiki/pull/291): Use placeholders in the logger message
    * [#292](https://github.com/apache/jspwiki/pull/292): `Serializable` classes without `serialVersionUID`

* Dependency updates
    * Commons Lang to 3.13.0
    * Flexmark to 0.64.8 (closes [#286](https://github.com/apache/jspwiki/pull/286)), thanks to dependabot
    * Hslqdb to 2.7.2 (closes [#287](https://github.com/apache/jspwiki/pull/287)), thanks to dependabot
    * JUnit to 5.10.0
    * Lucene to 9.7.0
    * Mockito to 5.4.0 (closes [#288](https://github.com/apache/jspwiki/pull/288)), thanks to dependabot
    * Selenide to 6.16.1 (closes [#294](https://github.com/apache/jspwiki/pull/294)), thanks to dependabot
    * Tomcat to 9.0.78
    * Maven plugins: clean to 3.3.1

**2023-06-16  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.1-git-01_

* Fix Workflow screen, which has been borked since 2.11.0.M5 :-/

* Dependency updates
    * Commons IO to 2.13.0
    * Flexmark to 0.64.6 (closes [#279](https://github.com/apache/jspwiki/pull/279)), thanks to dependabot
    * Selenide to 6.15.0 (closes [#281](https://github.com/apache/jspwiki/pull/281)), thanks to dependabot
    * Tika to 2.8.0
    * Tomcat to 9.0.76 (closes [#275](https://github.com/apache/jspwiki/pull/275)), thanks to dependabot
    * Maven plugins: assembly to 3.6.0, dependency to 3.6.0, project-info-reports to 3.4.5, release to 3.0.1, source to 3.3.0, surefire to 3.1.2, war to 3.4.0, cargo-maven3-plugin to 1.10.7 (closes [#280](https://github.com/apache/jspwiki/pull/280), thanks to dependabot), maven-surefire-junit5-tree-reporter to 1.2.1 (closes [#277](https://github.com/apache/jspwiki/pull/277), thanks to dependabot)

* Merged [#283](https://github.com/apache/jspwiki/pull/283), provided by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* Snapshots are automatically deployed as part of the `Jenkinsfile` build

**2023-05-13  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.0-git-07_

* Rollback wro4j plugin version and configuration to 1.8.0, as current configuration was affected by [MDEP-863](https://issues.apache.org/jira/browse/MDEP-863), preventing Docker builds

* jspwiki-util and jspwiki-main don't pull commons-httpclient, as they weren't using it anymore (it is still pulled transitively, though) 

* Dependency updates
    * AWS kendra java sdk to 1.12.468 
    * Lucene to 9.6.0 (closes [#276](https://github.com/apache/jspwiki/pull/276)), thanks to dependabot
    * Selenide to 6.14.0 (closes [#274](https://github.com/apache/jspwiki/pull/274)), thanks to dependabot
    * Tomcat to 9.0.75 (closes [#275](https://github.com/apache/jspwiki/pull/275)), thanks to dependabot
    * Maven plugins: remote-resources to 3.1.0, surefire to 3.1.0 (closes [#273](https://github.com/apache/jspwiki/pull/273), thanks to dependabot)

**2023-05-05  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.0-git-06_

* Merged [#241](https://github.com/apache/jspwiki/pull/241), provided by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* Dependency updates
    * Flexmark to 0.64.4 (closes [#270](https://github.com/apache/jspwiki/pull/270) and [#271](https://github.com/apache/jspwiki/pull/271)), thanks to dependabot
    * Mockito to 5.3.1 (closes [#269](https://github.com/apache/jspwiki/pull/269)), thanks to dependabot

**2023-04-28  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.0-git-05_

* Merged [#211](https://github.com/apache/jspwiki/pull/211), [#240](https://github.com/apache/jspwiki/pull/240) and [#258](https://github.com/apache/jspwiki/pull/258), provided by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* Dependency updates
    * Gson to 2.10.1 (closes [#265](https://github.com/apache/jspwiki/pull/265)), thanks to dependabot
    * Jaxen to 2.0.0 (closes [#264](https://github.com/apache/jspwiki/pull/264)), thanks to dependabot
    * JUnit to 5.9.3
    * Selenide to 6.13.1
    * Tomcat to 9.0.74
    * Maven plugins: enforcer to 3.3.0 (closes [#266](https://github.com/apache/jspwiki/pull/266), thanks to dependabot), project-info-reports to 3.4.3

**2023-03-26  Dirk Frederickx (brushed AT apache DOT org)**

* Remove files incorrectly added to the code base.

**2023-03-24  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.0-git-04_

* ReferenceManager implementation can be set via `jspwiki.refManager` property on your `jspwiki[-custom].properties` file
    * The provided implementation is expected to have a constructor receiving only an `Engine`

* `.html`, `.htm` and `.js` attachments are now forcibly downloaded by default, if you want to open them rather than 
downloading them, set the `jspwiki.attachment.forceDownload` property to empty on your `jspwiki[-custom].properties` file

* Fixed CSS file used by Install.jsp - now that's a pretty jsp to look at!

* Merged PRs [#231](https://github.com/apache/jspwiki/pull/231) and [#235](https://github.com/apache/jspwiki/pull/235), provided by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* Dependency updates
    * commons-fileupload to 1.5 (closes [#260](https://github.com/apache/jspwiki/pull/260)), thanks to dependabot
    * JUnit to 5.9.2
    * HSQLDB to 2.7.1 (closes [#244](https://github.com/apache/jspwiki/pull/244)), thanks to dependabot
    * jetty-all to 9.4.51.v20230217 (closes [#261](https://github.com/apache/jspwiki/pull/261)), thanks to dependabot
    * Log4j2 to 2.20.0
    * Lucene to 9.5.0
    * Mockito to 5.2.0
    * Selenide to 6.12.4
    * Tika to 2.7.0
    * Tomcat to 9.0.73
    * XStream on antrun plugin on portable module to 1.4.20 (closes [#256](https://github.com/apache/jspwiki/pull/256)), thanks to dependabot
    * Maven plugins: assembly to 3.5.0, compiler to 3.11.0, dependency to 3.5.0, enforcer to 3.2.1, install to 3.1.1, javadoc to 3.5.0, 
      project info reports to 3.4.2, release to 3.0.0, resources to 3.3.1, surefire to 3.0.0, cargo to 1.10.6, jspc to 3.2.2 (closes [#257](https://github.com/apache/jspwiki/pull/257), thanks to dependabot)

**2023-01-04  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.0-git-03_

* Fix flaky caching test, add some colouring to Jenkins console builds

* Dependency updates
    * Apache parent to 29
    * Javadoc umldoclet to 2.1.0
    * Lucene to 9.4.2
    * Mockito to 4.11.0
    * Selenide to 6.11.0
    * Tika to 2.6.0
    * Tomcat to 9.0.70
    * Maven plugins: dependency to 3.4.0, install to 3.1.0, release to 3.0.0-M7

**2022-11-24  Dirk Frederickx (brushed AT apache DOT org)**

* _2.12.0-git-02_

* Fixing a number of XSS vulnerabilities reported by Eugene Lim and Sng Jay Kai, from Government Technology Agency of Singapore.
  (sanitizing various plugin parameters)

**2022-10-19  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.12.0-git-01_

* Require at least Java 11 to build & run

* Add missing licenses PR [#220](https://github.com/apache/jspwiki/pull/220) provided by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* Dependency updates
    * commons-text to 1.10.0
    * Flexmark to 0.64.0
    * Gson to 2.9.1 (closes [#219](https://github.com/apache/jspwiki/pull/219)), thanks to dependabot
    * JUnit to 5.9.1
    * Log4J to 2.19.0
    * Lucene to 9.4.0
    * Mockito to 4.8.1 (closes/superseeds [#225](https://github.com/apache/jspwiki/pull/225))
    * NekoHTML to 2.1.2
    * Selenide to 6.9.0 (closes [#233](https://github.com/apache/jspwiki/pull/233)), thanks to dependabot
    * Tika to 2.5.0
    * Tomcat to 9.0.68
    * Maven plugins: jar to 3.3.0, javadoc to 3.4.1, jxr to 3.3.0, project-info-reports to 3.4.1 (closes [#224](https://github.com/apache/jspwiki/pull/224), thanks to dependabot), wro4j to 1.10.1

**2022-07-25  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.3-git-12_

* Dependency updates
    * JUnit to 5.9.0 (closes [#218](https://github.com/apache/jspwiki/pull/218)), thanks to dependabot
    * HSQLDB to 2.7.0 (closes [#217](https://github.com/apache/jspwiki/pull/217)), thanks to dependabot
    * Tomcat to 9.0.65 (closes [#210](https://github.com/apache/jspwiki/pull/210)), thanks to dependabot 
    * Maven plugins: install to 3.0.1 (closes [#214](https://github.com/apache/jspwiki/pull/214)), assembly to 3.4.2 (closes [#215](https://github.com/apache/jspwiki/pull/215)), surefire junit5 tree reporter to 1.1.0 (closes [#216](https://github.com/apache/jspwiki/pull/216)), thanks to dependabot

**2022-07-20  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.3-git-11_

* Bring explicit CSRF protection to user management JSPs

* Merged PRs with several code improvements [#202](https://github.com/apache/jspwiki/pull/202), [#203](https://github.com/apache/jspwiki/pull/203), [#204](https://github.com/apache/jspwiki/pull/204), [#205](https://github.com/apache/jspwiki/pull/205) and [#206](https://github.com/apache/jspwiki/pull/206) provided by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* Dependency updates
    * Maven plugins: project-info-reports to 3.4.0 (closes [#208](https://github.com/apache/jspwiki/pull/208), thanks to dependabot)

**2022-07-14  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.3-git-10_

* Bring explicit CSRF protection to group management JSPs

* Add default application name on `<title>` elements to templates' templates, and `Main` as default frontpage is none is defined on default template

* [`org.apache.wiki.markdown.migration.WikiSyntaxConverter`](https://github.com/apache/jspwiki/blob/master/jspwiki-markdown/src/test/java/org/apache/wiki/markdown/migration/WikiSyntaxConverter.java) now also brings pages' attachments when converting to markdown

* Dependency updates
    * Parent to Apache Parent 27
    * Jetty to 9.4.48.v20220622 - closes [#199](https://github.com/apache/jspwiki/pull/199), thanks to dependabot
    * Mockito to 4.6.1 - closes [#198](https://github.com/apache/jspwiki/pull/198), thanks to dependabot
    * Maven plugins: release to 3.0.0-M6 (closes [#201](https://github.com/apache/jspwiki/pull/201), thanks to dependabot), junit5-tree-reporter to 1.0.1 (closes [#200](https://github.com/apache/jspwiki/pull/200), thanks to dependabot)

**2022-07-12  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.3-git-09_

* New `CsrfProtectionFilter` to protect POST requests from CSRF attacks
    * `org.apache.wiki.api.core.Session` gains new `String antiCsrfToken()` method 

* [`org.apache.wiki.markdown.migration.WikiSyntaxConverter`](https://github.com/apache/jspwiki/blob/master/jspwiki-markdown/src/test/java/org/apache/wiki/markdown/migration/WikiSyntaxConverter.java) to convert from JSPWiki syntax to markdown
    * does not keep pages' history nor attachments (yet), but it's enough to generate the basic set of wikipages from jspwiki-wikipages artifacts

* Fixed logout modal dialog not showing up

* `TestEngine` is able to not clean up directories / ensure they have unique names if `jspwiki.test.disable-clean-props` wiki property is set to `true` 

* Fixed integration tests
    * Upgrade to cargo-maven3 broke the launch configuration
    * Latest Selenide upgrades broke SearchIT, which wasn't hovering on search form

* Merged PRs with several code improvements [#192](https://github.com/apache/jspwiki/pull/192), [#193](https://github.com/apache/jspwiki/pull/193), [#195](https://github.com/apache/jspwiki/pull/195) and [#196](https://github.com/apache/jspwiki/pull/196), provided by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* Dependency updates
    * Log4J to 2.18.0
    * Lucene to 8.11.2
    * Selenide to 6.6.6
    * Tika to 2.4.1 - closes [#187](https://github.com/apache/jspwiki/pull/187)
    * Tomcat to 9.0.64 - closes [#189](https://github.com/apache/jspwiki/pull/189)
    * Maven plugins: antrun to 3.1.0  (closes [#190](https://github.com/apache/jspwiki/pull/190), thanks to dependabot), assembly to 3.4.1, enforcer to 3.1.0, surefire to 3.0.0-M7, cargo-maven3 to 1.9.13 (closes [#197](https://github.com/apache/jspwiki/pull/197), thanks to dependabot)

**2022-05-01  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.3-git-08_

* `TestEngine#shutdown` now cleans cache, wiki pages dir and their associated attachments, if any. This allows cleaning up a handful of tests
    * `TestEngine#emptyWikiDir` deletes both page and attachment directories

* Dependency updates
    * Jetty to 9.4.46.v20220331 - closes [#181](https://github.com/apache/jspwiki/pull/181), thanks to dependabot
    * Mockito to 4.5.1 - closes [#185](https://github.com/apache/jspwiki/pull/185), thanks to dependabot
    * Maven project info reports plugin to 3.3.0 - closes [#186](https://github.com/apache/jspwiki/pull/186), thanks to dependabot

**2022-04-24  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.3-git-07_

* Ensure Lucene indexes all pages and attachments, even when they don't fit in the cache. Should fix [JSPWIKI-1171](https://issues.apache.org/jira/browse/JSPWIKI-1171)

* Add a memory profiling test, so it can be used to estimate the cache configuration & other memory requirements. To run it just `mvn test -Dtest=MemoryProfiling` on the `jspwiki-main` module

* Dependency updates
    * Awaitility to 4.2.0
    * Apache parent pom to 26 - closes [#182](https://github.com/apache/jspwiki/pull/182), thanks to dependabot
    * Maven javadoc plugin to 3.4.0
    * Mockito to 4.5.0
    * Selenide to 6.4.0 - closes [#183](https://github.com/apache/jspwiki/pull/183), thanks to dependabot

**2022-03-28  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.3-git-06_

* `DefaultUserManager#validateProfile`: requires always a non-null password in order to avoid CSRF attacks

* Fixing italian locale - PR [#173](https://github.com/apache/jspwiki/pull/173), provided by [Thiago Coutinho](https://github.com/selialkile), thanks!

* Dependency updates
    * Tomcat to 9.0.62
    * Maven clean plugin to 3.2.0 
    * Maven surefire plugin to 3.0.0-M6, now with JUnit 5 tree reporter - closes [#180](https://github.com/apache/jspwiki/pull/180), thanks to dependabot

**2022-03-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.3-git-05_

* Weblog plugin: sanities the plugin output to protect against Xss attacks.


**2022-03-22  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.3-git-04_

* [JSPWIKI-802](https://issues.apache.org/jira/browse/JSPWIKI-802) - Markdown syntax Support: initial support for plain editor.
    * Currently, can be activated by setting the `jspwiki.syntax.plain` property to `plain/wiki-snips-markdown.js`.
    * Alternatively, as of 2.11.3-git-02, whole Markdown support configuration can be set up by setting the `jspwiki.syntax` property to `markdown`.
    * Details at [Markdown support page](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Markdown%20Support).

* `TemplateManager` now understands resources beginning with `engine://` to be keys of `Engine`'s wiki properties. In those cases, if the key and its associated value exist, the latter will be used as the requested resource.

* Dependency updates
    * Mockito to 4.4.0 - closes [#176](https://github.com/apache/jspwiki/pull/176), thanks to dependabot
    * Selenide to 6.3.5
    * Tomcat to 9.0.60
    * XStream to 1.4.19 - closes [#177](https://github.com/apache/jspwiki/pull/177), thanks to dependabot
    * Maven JXR plugin to 3.2.0, dependency plugin to 3.3.0 - closes [#178](https://github.com/apache/jspwiki/pull/178), thanks to dependabot


**2022-03-15  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.3-git-03_

* Denounce plugin: sanities the plugin attributes to protect against Xss attacks.


**2022-03-11  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.3-git-02_

* Introduce [Engine Lifecycle Extensions](https://jspwiki-wiki.apache.org/Wiki.jsp?page=HowToWriteAnEngineLifecycleExtension) into JSPWiki's [public API](https://jspwiki-wiki.apache.org/Wiki.jsp?page=JSPWikiPublicAPI).

* Extracted `org.apache.wiki.htmltowiki` to its own module, `jspwiki-wysiwyg`

* Dependency updates
    * Apache parent pom to 25 - closes [#172](https://github.com/apache/jspwiki/pull/172), thanks to dependabot
    * AWS Kendra Java SDK to 1.12.176
    * Gson to 2.9.0
    * Jetty to 9.4.45.v20220203 - closes [#175](https://github.com/apache/jspwiki/pull/175), thanks to dependabot
    * Log4J to 2.17.2
    * Nekohtml migrated to [CodeLibs' fork](https://github.com/codelibs/nekohtml), version 2.0.2
    * Selenide to 6.3.4
    * Tomcat to 9.0.59
    * Maven compiler plugin to 3.10.1, project info reports plugin to 3.2.2 - closes [#174](https://github.com/apache/jspwiki/pull/174), thanks to dependabot


**2022-03-05  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.3-git-01_

* Fix for Xss vulnerability on XHRHtml2Markup.jsp.  Adding additional protection against
  malicious injection of invalid html/xml.


**2022-02-20  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.2-git-04_

* [JSPWIKI-79](https://issues.apache.org/jira/browse/JSPWIKI-79) - Ounce Labs Security Finding: Authentication - Change Password

* Refactored the `org.apache.wiki.htmltowiki` package, so it doesn't use `Stack` but instead a `Deque`. May split the package on the main module to its own module later on.

* Dependency updates
    * Mockito 4.3.1
    * Selenide to 6.3.3 - closes [#168](https://github.com/apache/jspwiki/pull/168)
    * SLF4J to 1.7.36
    * Tika to 2.3.0 - closes [#170](https://github.com/apache/jspwiki/pull/170)
    * Tomcat to 9.0.58
    * Maven compiler (3.10.0), project info reports (3.2.1 - closes [#169](https://github.com/apache/jspwiki/pull/169)), javadocs (3.3.2) and cargo-maven3 (1.9.10) plugins


**2022-01-13  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.2-git-03_

* Added [DefinitionExtension](https://github.com/vsch/flexmark-java/wiki/Extensions#definition-lists) and [TablesExtension](https://github.com/vsch/flexmark-java/wiki/Extensions#tables) to `jspwiki-markdown` in order to add support for definition lists and tables.

* [JSPWIKI-802](https://issues.apache.org/jira/browse/JSPWIKI-802) - Markdown syntax Support: added Markdown support for WYSIWYG editor.
    * Currently, can be activated by setting the `jspwiki.syntax.decorator` property to `org.apache.wiki.htmltowiki.syntax.markdown.MarkdownSyntaxDecorator`.
    * Details at [Markdown support page](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Markdown%20Support).
    * Last item pending for full Markdown support is Plain Editor integration.

* `XMLUserDatabase#getWikiNames()` now discards null and empty wiki names.
    * It was discarding only `null` wiki names, but JDom returns an empty string (that is, not null) for missing attributes, which resulted in unreachable code.
    * This change inlines with the logic of the UI on the registration form, which mandates a not empty value for the wiki name.
    * Same on `JDBCUserDatabase#getWikiNames()` which seems to have been developed from the former (log message references XMLUserDatabase).

* Dependency updates
    * Selenide to 6.2.0, thanks to dependabot [#165](https://github.com/apache/jspwiki/pull/165)
    * Maven release (3.0.0-M5) and cargo plugins (1.9.9), thanks to dependabot [#164](https://github.com/apache/jspwiki/pull/164), [#166](https://github.com/apache/jspwiki/pull/166)
    * Maven compiler (3.9.0) and jar (3.2.2) plugins


**2022-01-12  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.2-git-02_

* Protect the meta 'wikiUserName' tag against potential XSS attack.
  (reported by Paulos Yibelo)


**2021-12-31  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.2-git-01_

* [JSPWIKI-1168](https://issues.apache.org/jira/projects/JSPWIKI/issues/JSPWIKI-1168) - Simplify required configuration to log on file: Added an unused rolling file appender configuration to `jspwiki.properties`, so switching log to file only requires referencing/overwritting a bit of configuration.

* Added [AttributesExtension](https://github.com/vsch/flexmark-java/wiki/Extensions#attributes) to `jspwiki-markdown` in order to add support for [Markdown Extra attributes](https://michelf.ca/projects/php-markdown/extra/#spe-attr).

* [JSPWIKI-1169](https://issues.apache.org/jira/projects/JSPWIKI/issues/JSPWIKI-1169) - Add Bill of materials module to build.

* `DefaultReferenceManager` now only synchronizes when (un)serializing data, since the underlying maps used are already handling concurrency.

* Some small refactors on htmltowiki decorators. Most notably, `<a>` syntax decorator only performs tasks related to syntax decoration.

* Dependency updates
    * Log4J2 to 2.17.1, thanks to dependabot [#161](https://github.com/apache/jspwiki/pull/161)
    * Lucene to 8.11.1, thanks to dependabot [#162](https://github.com/apache/jspwiki/pull/162)
    * Mockito to 4.2.0, thanks to dependabot [#160](https://github.com/apache/jspwiki/pull/160)
    * Selenide to 6.1.2
    * Tika to 2.2.1, thanks to dependabot [#163](https://github.com/apache/jspwiki/pull/163)


**2021-12-13  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.1-git-02_

* Decoupled `XHtmlElementToWikiTranslator` from jspwiki syntax, so it will be able in a near future to output other wiki syntaxes.
    * `XHtmlElementToWikiTranslator` acts as a chain in a chain of responsability pattern, delegating to a `SyntaxDecorator` the output of specific wiki syntaxes.
    * Refactored classes may still change a little.

* Dependency updates
    * Mockito to 4.1.0, thanks to dependabot [#152](https://github.com/apache/jspwiki/pull/152)
    * Log4J2 to 2.15.0, thanks to [Paulino Calderon](https://github.com/cldrn) [#155](https://github.com/apache/jspwiki/pull/155) and then to 2.16.0, thanks to dependabot [#157](https://github.com/apache/jspwiki/pull/157)
    * Sonar maven plugin to 3.9.1.2184, thanks to dependabot [#153](https://github.com/apache/jspwiki/pull/153)
    * Tomcat to 9.0.56

**2021-12-02  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.1-git-01_

* Cache management moved to a new maven module, jspwiki-cache
    * Cache backend can now be overriden by providing a custom CachingManager via [classmappings-extra.xml](https://jspwiki-wiki.apache.org/Wiki.jsp?page=JSPWikiPublicAPI#section-JSPWikiPublicAPI-RegisteringCustomManagersInTheWikiEngine)
    * Default cache manager remains ehcache-based, with default configuration file located at ehcache-jspwiki.xml
    * Tests wanting to invalidate cache(s) should call either `Engine#shutdown()` or `Engine#getManager( CachingManager.class ).shutdown()`
    * The `jspwiki.cache.config-file` setting on the `jspwiki[-custom].properties` file allows to use a custom ehcache configuration file, located elsewhere on classpath
    * Fixed [JSPWIKI-873](https://issues.apache.org/jira/projects/JSPWIKI/issues/JSPWIKI-873) - AttachmentManager#getAllAttachments() does not return more than exactly 1000 attachments

* Introduced `TextUtil#get[Required|String]Property( Properties, String key, String deprecatedKey[, String defval] )` to allow deprecation of properties, so they can be removed later on
    * Deprecated key will be looked first and, if found, a warning will be logged asking to move to the new property
    * If there's no deprecated key on the properties set, the normal key will be looked, and if not found, the default value will be returned (or exception thrown)
    * The idea is to move related configuration towards common "namespaces"
    * A few properties are deprecated
        * `jspwiki.usePageCache` -> `jspwiki.cache.enable` should be used instead
        * `jspwiki.attachmentProvider` -> `jspwiki.attachment.provider` should be used instead
        * `jspwiki.attachmentProvider.adapter.impl` -> `jspwiki.attachment.provider.adapter.impl` should be used instead

* `WikiEngine#initComponent()` now asks the `mappedClass` if it is `Initializable` instead of asking the `requestedClass` on `classmappings.xml`.
    * This allows to decouple `Initializable` from the mapped managers, as it should only matter if their implementations are `Initializable` in order to init them.

* Moved site generation to [jspwiki-site's Jenkinsfile](https://github.com/apache/jspwiki-site/blob/jbake/Jenkinsfile)
    * This second build is decoupled from the main one, so CI feedback is gathered faster

* Dockerfile's maven build does not rely on jspwiki-main:tests being available on a repo, thus avoiding [#1](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Common%20problems%20when%20building%20JSPWiki#section-Common+problems+when+building+JSPWiki-JspwikiMainJarTestsX.Y.ZNotFoundAtJspwikiMarkdown) when building new versions

* Dependency updates
    * Awaitility to 4.1.1, thanks to dependabot [#152](https://github.com/apache/jspwiki/pull/152)
    * JUnit to 5.8.2
    * Selenide to 6.1.1

**2021-11-18  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-14_

* [JSPWIKI-1160](https://issues.apache.org/jira/browse/JSPWIKI-1160) - Ensure JSPWiki builds with JDKs 8, 11 and 17

* Dependency updates
    * Lucene to 8.11.0

**2021-11-17  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-13_

* [JSPWIKI-1159](https://issues.apache.org/jira/browse/JSPWIKI-1159) - [ReferredPagesPlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=ReferredPagesPlugin), [ReferringPagesPlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=ReferringPagesPlugin), [ReferringUndefinedPagesPlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=ReferringUndefinedPagesPlugin), [UndefinedPagesPlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=UndefinedPagesPlugin) and [UnusedPagesPlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=UnusedPagesPlugin) now accept a `columns` parameter to split the results into

* `CookieAuthenticationLoginModule#clearLoginCookie` ensures that the cookie to be deleted belongs to the logincookie directory

* Fixed login when using `CookieAuthenticationLoginModule`, http response was being written after being committed

* Fixed Dockerfile/JDK8 build

* Dependency updates
    * cargo-maven2-plugin to cargo-maven3-plugin
    * hsqldb to 2.6.1, thanks to dependabot [#151](https://github.com/apache/jspwiki/pull/151)
	* Tomcat to 9.0.55

**2021-10-28  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-12_

* [#148](https://github.com/apache/jspwiki/pull/148) - Added missing translation de/german - Thanks to [Peter Hormanns](https://github.com/phormanns)!

* Several code improvements, all relevant PRs provided by [Arturo Bernal](https://github.com/arturobernalg), thanks!
    * [JSPWIKI-1155](https://issues.apache.org/jira/browse/JSPWIKI-1155) - String concat in StringBuilder [#140](https://github.com/apache/jspwiki/pull/140)
    * [JSPWIKI-1156](https://issues.apache.org/jira/browse/JSPWIKI-1156) - Remove `protected` member in `final` class [#141](https://github.com/apache/jspwiki/pull/141)
    * [JSPWIKI-1157](https://issues.apache.org/jira/browse/JSPWIKI-1157) - Remove redundant String [#142](https://github.com/apache/jspwiki/pull/142)
    * [JSPWIKI-1158](https://issues.apache.org/jira/browse/JSPWIKI-1158) - Remove unnecessary ToString [#147](https://github.com/apache/jspwiki/pull/147)

* Dependency updates
    * Gson to 2.8.9, thanks to dependabot [#150](https://github.com/apache/jspwiki/pull/150)
    * Lucene to 8.10.1, thanks to dependabot [#149](https://github.com/apache/jspwiki/pull/149)
    * Selenide to 6.0.3 [#145](https://github.com/apache/jspwiki/pull/145)

**2021-10-16  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-11_

* [JSPWIKI-1140](https://issues.apache.org/jira/browse/JSPWIKI-1140) - Autogenerate changenote on page comments

* [JSPWIKI-1149](https://issues.apache.org/jira/browse/JSPWIKI-1149) - Missing legacy Lucene codec [#143](https://github.com/apache/jspwiki/pull/143)

* First stab at `XHtmlElementToWikiTranslator` refactor, so it'll be easier in the future to make it output other types of wiki syntaxes

* Dependency updates, provided by dependabot
    * Jetty-all to 9.4.44.v20210927 [#139](https://github.com/apache/jspwiki/pull/139)
    * Lucene to 8.10.0 [#143](https://github.com/apache/jspwiki/pull/143)
    * Mockito to 4.0.0 [#144](https://github.com/apache/jspwiki/pull/144)
    * Selenide to 5.25.0 [#138](https://github.com/apache/jspwiki/pull/138)
    * Tomcat to 9.0.54

**2021-09-27  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-10_

* Several code improvements, all relevant PRs provided by [Arturo Bernal](https://github.com/arturobernalg), thanks!
    * [JSPWIKI-1148](https://issues.apache.org/jira/browse/JSPWIKI-1148) - Avoid File Stream [#47](https://github.com/apache/jspwiki/pull/47)
    * [JSPWIKI-1151](https://issues.apache.org/jira/browse/JSPWIKI-1151) - Simplify assertion with simpler and equivalent calls [#134](https://github.com/apache/jspwiki/pull/134)
    * [JSPWIKI-1152](https://issues.apache.org/jira/browse/JSPWIKI-1152) - Make final variable when is possible [#135](https://github.com/apache/jspwiki/pull/135)
    * [JSPWIKI-1153](https://issues.apache.org/jira/browse/JSPWIKI-1153) - Inline Variable [#136](https://github.com/apache/jspwiki/pull/136)
    * [JSPWIKI-1154](https://issues.apache.org/jira/browse/JSPWIKI-1154) - Replace ´if´ with switch statements [#137](https://github.com/apache/jspwiki/pull/137)

* Dependency updates, provided by dependabot
    * JUnit to 5.8.1 [#132](https://github.com/apache/jspwiki/pull/132)
    * Selenide to 5.24.4 [#131](https://github.com/apache/jspwiki/pull/131)
    * Tomcat to 9.0.53 [#127](https://github.com/apache/jspwiki/pull/127)
    * XStream to 1.4.18 [#128](https://github.com/apache/jspwiki/pull/128)

**2021-09-11  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-09_

* Docker images pushed to Docker Hub

* Dependency updates
    * Commons IO to 2.11.0 [#125](https://github.com/apache/jspwiki/pull/125)
    * GSON to 2.8.8 [#126](https://github.com/apache/jspwiki/pull/126)
    * Mockito to 3.12.4 [#123](https://github.com/apache/jspwiki/pull/123)
    * Selenide to 5.24.2
    * Some maven plugins [#121](https://github.com/apache/jspwiki/pull/121), [#122](https://github.com/apache/jspwiki/pull/122), [#124](https://github.com/apache/jspwiki/pull/124)

**2021-09-01  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-08_

* [JSPWIKI-1143](https://issues.apache.org/jira/browse/JSPWIKI-1143) - Allow SpamFilter to exclude certain users/groups from checks
    * `jspwiki.filters.spamfilter.allowedgroups` property can be used to set a comma separated list of groups that will bypass the filter

* Denounce plugin checks for valid URLs

* Dependency updates
    * ASF parent pom to 24
    * AWS java sdk kendra to 1.12.59
    * EhCache to 2.10.9.2
    * Jetty to 9.4.43.v20210629 [#117](https://github.com/apache/jspwiki/pull/117)
    * Lucene to 8.9.0 [#115](https://github.com/apache/jspwiki/pull/115)
    * Selenide to 5.24.1
    * Tika to 2.1.0
    * Tomcat to 9.0.52
    * Some maven plugins [#58](https://github.com/apache/jspwiki/pull/58), [#114](https://github.com/apache/jspwiki/pull/114), [#119](https://github.com/apache/jspwiki/pull/119), [#120](https://github.com/apache/jspwiki/pull/120)

**2021-07-30  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-07_

* [JSPWIKI-795](https://issues.apache.org/jira/browse/JSPWIKI-795) - Update Logging subsystem to Log4J2
    * Replace all Log4J code with Log4J2.

* `PropertyReader#loadWebAppProps( ServletContext )` now takes the following properties sources:
    * 1.- Default JSPWiki properties
    * 2.- System environment
    * 3.- JSPWiki custom property files
    * 4.- JSPWiki cascading properties
    * 5.- System properties
    * With the later ones taking precedence over the previous ones. To avoid leaking system information, only System
      environment and properties beginning with `jspwiki` (case unsensitive) are taken into account.
    * Also, to ease docker integration, System env properties containing "_" are turned into ".". F.ex.,
      `ENV jspwiki_fileSystemProvider_pageDir` would be loaded as `jspwiki.fileSystemProvider.pageDir`.

* Minor Dockerfile updates

* Dependency updates
    * Selenide to 5.23.1
    * SLF4J to 1.7.32
    * Tika to 2.0.0

**2021-07-12  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-06_

* [JSPWIKI-795](https://issues.apache.org/jira/browse/JSPWIKI-795) - Update Logging subsystem to Log4J2
    * Log4J2 is the new logging framework used by JSPWiki. Although all Log4J calls are transparently routed to Log4J2,
      the configuration inside jspwiki.properties has changed, so installations with customized logging configuration will
      need to be set up again.
    * Existing 3rd party plugins, filters and providers will continue to work as expected, as Log4J calls will be routed
      to Log4J2, but the use of Log4J2 should be preferred onwards.
    * Note that Log4J calls are still used throughout JSPWiki; they'll be replaced by equivalent Log4J2 calls later on,
      but this will be an implementation detail that should be transparent to end users.
    * This should be the last breaking change towards 2.11.0.

* Dependency updates
    * Awaitility to 4.1.0
    * AWS Kendra to 1.12.21 [#113](https://github.com/apache/jspwiki/pull/113)
    * Commons Lang to 3.12.0 [#61](https://github.com/apache/jspwiki/pull/61)
    * EhCache to 2.10.9.2 [#64](https://github.com/apache/jspwiki/pull/64)
    * JUnit to 5.7.2
    * Log4J replaced by Log4J2, 2.14.1
    * Lucene to 8.9.0 [#62](https://github.com/apache/jspwiki/pull/62)
    * Mockito to 3.11.2
    * Selenide to 5.22.2
    * Tika to 1.27
    * Tomcat to 9.0.50

**2021-04-24  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-05_

* [JSPWIKI-1145](https://issues.apache.org/jira/browse/JSPWIKI-1145) - Weak one-way hash used
    * Merged [PR #51](https://github.com/apache/jspwiki/pull/51), contributed by [takalat](https://github.com/takalat), [samhareem](https://github.com/samhareem), thanks!

* Dependency & plugin updates provided by dependabot (PRs [#34](https://github.com/apache/jspwiki/pull/34), [#35](https://github.com/apache/jspwiki/pull/35), [#39](https://github.com/apache/jspwiki/pull/39), [#52](https://github.com/apache/jspwiki/pull/52), [#55](https://github.com/apache/jspwiki/pull/55), [#56](https://github.com/apache/jspwiki/pull/56), [#57](https://github.com/apache/jspwiki/pull/57) and [#59](https://github.com/apache/jspwiki/pull/59)), most notably
    * HSQLDB to 2.6.0
    * JUnit to 5.7.1
    * Mockito to 3.9.0
    * Selenide to 5.20.4
    * Tomcat to 9.0.45

**2021-03-09  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-04_

* [JSPWIKI-1146](https://issues.apache.org/jira/browse/JSPWIKI-1146) - Add [AWS Kendra as a Search Provider](https://jspwiki-wiki.apache.org/Wiki.jsp?page=KendraSearchProvider)
    * Merged [PR #46](https://github.com/apache/jspwiki/pull/46), contributed by Julien Masnada, thanks!

* [JSPWIKI-1144](https://issues.apache.org/jira/browse/JSPWIKI-1144) - Minor performance improvement
    * Merged [PR #36](https://github.com/apache/jspwiki/pull/36), contributed by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* [JSPWIKI-1147](https://issues.apache.org/jira/browse/JSPWIKI-1147) - The button "Clear user preferences"
doesn't clear user preferences

**2021-01-11  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-03_

* [JSPWIKI-1142](https://issues.apache.org/jira/browse/JSPWIKI-1142) - Minor performance improvements
    * Merged [PR #32](https://github.com/apache/jspwiki/pull/32), contributed by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* `PropertyReader` logs stacktrace if unable to load the `jspwiki.properties` file

* `WikiEngine` unregisters all event delegates from `WikiEventManager` on shutdown. Under some circumstances, unit tests
using a `TestEngine` could end up processing events using managers registered by previous `TestEngine`s.

* Tests using in-memory `Hsql` servers start them on random ports, in order to allow concurrent builds.

* Dependency updates
    * Selenide to 5.17.3

**2021-01-01  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-02_

* Dependency updates provided by dependabot (PRs [#18](https://github.com/apache/jspwiki/pull/18), [#19](https://github.com/apache/jspwiki/pull/19), [#20](https://github.com/apache/jspwiki/pull/20), [#21](https://github.com/apache/jspwiki/pull/21), [#22](https://github.com/apache/jspwiki/pull/22), [#23](https://github.com/apache/jspwiki/pull/23), [#24](https://github.com/apache/jspwiki/pull/24), [#25](https://github.com/apache/jspwiki/pull/25), [#26](https://github.com/apache/jspwiki/pull/26), [#27](https://github.com/apache/jspwiki/pull/27), [#28](https://github.com/apache/jspwiki/pull/28), [#29](https://github.com/apache/jspwiki/pull/29) and [#30](https://github.com/apache/jspwiki/pull/30)), most notably
    * jsp-api to 2.3.3
    * Selenide to 5.17.2

**2020-12-17  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-git-01_

* [JSPWIKI-1135](https://issues.apache.org/jira/browse/JSPWIKI-1135) - Add dependabot config file
    * Merged [PR #15](https://github.com/apache/jspwiki/pull/15), contributed by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* [JSPWIKI-1136](https://issues.apache.org/jira/browse/JSPWIKI-1136) - Refine Class member fields
    * Merged [PR #16](https://github.com/apache/jspwiki/pull/16), contributed by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* [JSPWIKI-1137](https://issues.apache.org/jira/browse/JSPWIKI-1137) - Minor Improvement
    * Merged [PR #17](https://github.com/apache/jspwiki/pull/17), contributed by [Arturo Bernal](https://github.com/arturobernalg), thanks!

* Dependency updates
    * Gson to 2.8.6
    * Tomcat to 9.0.41

**2020-12-01  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M8-git-06_

* Completed french translation - thanks to Ainara González Pérez!

* Dependency updates
    * Tika to 1.25

**2020-11-29  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M8-git-05_

* [JSPWIKI-1134](https://issues.apache.org/jira/browse/JSPWIKI-1134): german translation improved
    * Patches provided by Dietrich Schmidt - thanks!

* Extracted hidden input fields expected by the SpamFilter into its own custom tag and applied it to editor-related JSPs.

* Ensure IndexPlugin works with non-blank page references.

* Use ConcurrentHashMap inside DefaultReferenceManager, to avoid possible thread safety issues.

* Dependency updates
    * Selenide 5.16.2
    * Tomcat to 9.0.40

**2020-11-10  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M8-git-04_

* [JSPWIKI-1131](https://issues.apache.org/jira/browse/JSPWIKI-1131): Lucene Index not updated on edits/new page
    * Ensure latest version of page gets indexed, so changes using `VersioningFileProvider` get returned by searches.

* Removed unused publishers from `Jenkinsfile` in order to speed up the CI build.

**2020-11-07  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M8-git-03_

* Added functional tests for page edits and page searches.

* Simplified generation of portable build native binaries, a simple `mvn clean install -Dgenerate-native-launchers=true`
  is all that is needed to generate the binaries. As this takes some more time, is only needed when upgrading tomcat and
  needs to download artifacts not present on Maven's central repo, it is not enabled by default.

* Jenkinsfile uses JDK 11 to perform the build, as this is now the minimum [required by SonarQube](https://sonarcloud.io/documentation/appendices/end-of-support/).
  The build itself still requires at least JDK 1.8.

* Dependency updates
    * Cargo plugin to 1.8.2
    * Lucene to 8.7.0
    * Mockito 3.6.0
    * Selenide 5.15.1
    * Tomcat to 9.0.39

**2020-10-14  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M8-git-02_

* [JSPWIKI-1114](https://issues.apache.org/jira/browse/JSPWIKI-1114): Show only part of Weblog entry on the overview page.
    * Preview parameter cutting now only at newlines, patch suggested by Ulf Dittmer - thanks!

* LuceneSearchProvider using now NIOFSDirectory instead of (deprecated) SimpleFSDirectory. Also, all Lucene's index
  writes are synchronized, whereas reads are not. This should help with issues noted at [JSPWIKI-1131](https://issues.apache.org/jira/browse/JSPWIKI-1131).

* _2.11.0-M8-git-01_

* [JSPWIKI-1131](https://issues.apache.org/jira/browse/JSPWIKI-1131): Lucene Index not updated on edits/new page

* Dependency updates
    * Awaitility to 4.0.3
    * Commons IO to 2.8.0
    * Commons Lang to 3.11
    * Commons Text to 1.9
    * Flexmark to 0.62.2
    * Hsqldb to 2.5.1
    * JUnit to 5.7.0
    * Lucene to 8.6.2
    * Mockito 3.5.13
    * Selenide 5.15.0
    * Tomcat to 9.0.38

**2020-05-21  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-20_

* Dependency updates
    * Flexmark to 0.61.32
    * Tomcat to 9.0.35

**2020-04-09  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-19_

* [JSPWIKI-304](https://issues.apache.org/jira/browse/JSPWIKI-304): Workflows are not Serializable

* Dependency updates
    * Flexmark to 0.61.26
    * JUnit to 5.6.2
    * Lucene to 8.5.1
    * Selenide to 5.11.1
    * Tika to 1.24

**2020-04-09  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-18_

* [JSPWIKI-303](https://issues.apache.org/jira/browse/JSPWIKI-303): [JSPWiki API](https://jspwiki-wiki.apache.org/Wiki.jsp?page=JSPWikiPublicAPI) library creation
    * Promote `RenderingManager#textToHtml( Context, String )` to the public API

* Dependency updates
    * Tomcat to 9.0.34

**2020-03-29  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-17_

* Extracted Wiki initialization servlet context listener to its own module `jspwiki-bootstrap`
    * This module is responsible for the startup procedures of the application, before the Engine is created:
        * Locate and instantiate Wiki's SPIs implementations
        * Log configuration

* Begin to prepare [JSPWIKI-795](https://issues.apache.org/jira/projects/JSPWIKI/issues/JSPWIKI-795) - Update logging subsystem in JSPWiki
    * Log4J will now be configured only if present in classpath. Right now this means always, but once
    the logging subsystem is updated and in order to allow backwards compatibility with existing custom
    extensions, it will have to be explicitly added.

* Small usability improvement on login page: make tab key follow login form fields

* Dependency updates
    * Commons Lang to 3.10
    * Introduced Mockito 3.3.3 as mock testing library

**2020-03-29  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-16_

* [JSPWIKI-303](https://issues.apache.org/jira/browse/JSPWIKI-303): [JSPWiki API](https://jspwiki-wiki.apache.org/Wiki.jsp?page=JSPWikiPublicAPI) library creation
    * SPI to retrieve / create objects from the `o.a.w.api.core` package
    * it is possible to provide custom implementations of objects from the `o.a.w.api.core` package
        * for a custom `Engine`, an implementation of `o.a.w.api.spi.EngineSPI`, and set the
        `jspwiki.provider.impl.engine` property on the `jspwiki-[custom].properties` file with the
        fully qualified name of the implementation
        * for a custom `Context`, an implementation of `o.a.w.api.spi.ContextSPI`, and set the
        `jspwiki.provider.impl.context` property on the `jspwiki-[custom].properties` file with the
        fully qualified name of the implementation
        * for a custom `Session`, an implementation of `o.a.w.api.spi.SessionSPI`, and set the
        `jspwiki.provider.impl.session` property on the `jspwiki-[custom].properties` file with the
        fully qualified name of the implementation
        * for custom `Page` or `Attachment`, an implementation of `o.a.w.api.spi.ContentsSPI`, and set the
        `jspwiki.provider.impl.contents` property on the `jspwiki-[custom].properties` file with the
        fully qualified name of the implementation
        * for custom `Acl` or `AclEntry`, an implementation of `o.a.w.api.spi.AclsSPI`, and set the
        `jspwiki.provider.impl.acls` property on the `jspwiki-[custom].properties` file with the
        fully qualified name of the implementation

* [JSPWIKI-806](https://issues.apache.org/jira/browse/JSPWIKI-806) (EntityManager Proposal): add the possibility of loading custom managers on `WikiEngine`
    * `WikiEngine` will look on classpath for an `ini/classmappings-extra.xml` file, with the same structure as
    `ini/classmappings.xml`
    * if found, will register each `requestedClass` with its correspondent `mappedClass`
    * these custom manager must have a no-arg constructor
    * if there's a need to perform some initialization tasks querying the `Engine`, the custom manager should
    implement `o.a.w.api.engine.Initializable` and perform those tasks there

* `SisterSites.jsp` now honours page ACLs

**2020-03-25  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-15_

* [JSPWIKI-303](https://issues.apache.org/jira/browse/JSPWIKI-303): JSPWiki-API library creation
    * `Page` deals with ACLs

* Refactor `WikiEngine` initialization, in order to prepare for building and configuring custom
managers (somewhat related to [JSPWIKI-806](https://issues.apache.org/jira/browse/JSPWIKI-806) - EntityManager Proposal)

* Dependency updates
    * Lucene to 8.5.0
    * JUnit to 5.6.1

**2020-03-21  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-14_

* [JSPWIKI-303](https://issues.apache.org/jira/browse/JSPWIKI-303): JSPWiki-API library creation
    * added compatibility to page / attachment providers not using the public API
        * `jspwiki.pageProvider` should be set to `WikiPageAdapterProvider` and then `jspwiki.pageProvider.adapter.impl`
        to the actual page provider
        * `jspwiki.attachmentProvider` should be set to `WikiAttachmentAdapterProvider` and then `jspwiki.attachmentProvider.adapter.impl`
        to the actual attachment provider
        * see `WikiProviderAdaptersTest` on the jspwiki-210-adapters module for an example
    * `Page` does not deal with ACLs yet
    * SPI to create objects from the `o.a.w.api.core` package still needs to be done

* Added more helper methods to `TestEngine` to ease building customized instances (again, see `WikiProviderAdaptersTest` on the
jspwiki-210-adapters module for an example)

* Dependency updates
    * Tika to 1.24
    * Tomcat to 9.0.33
    * Selenide to 5.10.0

**2020-03-16  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-13_

* [JSPWIKI-303](https://issues.apache.org/jira/browse/JSPWIKI-303): JSPWiki-API library creation
    * Extracted `jspwiki-event` and `jspwiki-api` maven modules from `jspwiki-main`
    * Created `jspwiki-210-adapters` and `jspwiki-210-test-adaptees` maven modules to ensure backwards
    compatibility with custom plugin / filters / page providers not using public API
    * JSPWiki Plugins, Filters and Page Providers are using the public API
    * Use of `o.a.w.api.core.Command` instead of `o.a.w.ui.Command` and of `o.a.w.api.search.QueryItem` and
    `o.a.w.api.search.SearchResult` instead of their counterparts from the `o.a.w.search` package
    * Start to introduce `Page`, `Attachment` and `Context` instead of `WikiPage`, `WikiAttachment` and `WikiContext`
    * JSPWiki API still needs some polishing
        * `Page` does not deal with ACLs yet
        * SPI to create objects from the `o.a.w.api.core` package still needs to be done
        * `WikiPageProvider` and `WikiAttachmentProvider` backwards compatibility still needs to be done

**2020-03-06  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-12_

* [JSPWIKI-303](https://issues.apache.org/jira/browse/JSPWIKI-303): JSPWiki-API library creation
    * Extracted `o.a.w.api.core.Session` from `o.a.w.WikiSession`, and use it throughout the code
    * Removed `o.a.w.api.engine` package, moving the interfaces there to their appropiate packages.
    * Promote `o.a.w.Release` to the `o.a.w.api` package

* Internal classes' refactors in order to break some class / packages cycles.

* Updated Tomcat to 9.0.31 in order to get JDK 8 level to compile and run JSPs when using the Cargo
  plugin. Baseline is still servlet 3.1 (i.e.: Tomcat 8.x), though.

**2002-03-04  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M7-git-11_

* Links with target="_blank" can expose your site to performance and security issues.
  Add rel="noreferrer" as protect against this issue.


**2020-02-24  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-10_

* Finally, finished [JSPWIKI-120](https://issues.apache.org/jira/browse/JSPWIKI-120):
    * Use `Engine` instead of `WikiEngine` throughout the code as much as possible.
    * `URLConstructor#initialize(..)` receives an `Engine` instead of a `WikiEngine`.
    * `InitializablePlugin` and implementing classes receive an `Engine` instead of a `WikiEngine`.
    * `PageFilter`s receive an `Engine` instead of a `WikiEngine` on `initialize` method.
    * Rename + extract interfaces from `EditorManager`, `InternationalizationManager`, `SearchManager`,
    and `TemplateManager`.

* Updated Flexmark to 0.60.2

**2020-02-24  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-09_

* [JSPWIKI-120](https://issues.apache.org/jira/browse/JSPWIKI-120):
    * Use `Engine` inside `WikiContext`, `WikiSession`, `WikiPage`, `Attachment` and `SessionMonitor`.
        * e.g. `WikiContext#getEngine()` now returns an `Engine` instead of a `WikiEngine`. To retrieve a manager
        from it just use `Engine#getManager( DesiredManager.class )`. See implementations on `getXXXManager()`
        methods on `WikiEngine` for details.
    * `WikiProvider#initialize(..)` receives an `Engine` instead of a `WikiEngine`.
    * `Engine` gains an `adapt( Class< E > cls )`, to facilitate downcasting to `Engine` implementation classes.
    * Removed `Engine#getCurrentWatchDog()`, as it was a pass-through and introduced a package cycle; use instead
    `WatchDog#getCurrentWathDog( Engine )`.
    `o.a.wiki` and `o.a.w.event`. To obtain the `WikiEngine` reference from the event just use `getSrc()`
    * Rename + extract interfaces from `AttachmentManager`, `AuthenticationManager`, `AuthorizationManager`,
    `GroupManager` and `UserManager`.

**2020-02-20  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-08_

* [JSPWIKI-120](https://issues.apache.org/jira/browse/JSPWIKI-120): finally, extracted `Engine` interface from
`WikiEngine`. It will be part of JSPWiki public API later on.
    * removed direct reference to `WikiEngine` on `WikiEngineEvent`, in order to break package cycle between
    `o.a.wiki` and `o.a.w.event`. To obtain the `WikiEngine` reference from the event just use `getSrc()`
    * extract new `PluginElement` interface from `PluginContent` in order to break a package/class cycle between
    `PluginContent` and `ParserStagePlugin`

* Merged [PR #13](https://github.com/apache/jspwiki/pull/13) from [Kideath](https://github.com/kideath) in order
to fix russian translation. Thanks!

* Dependency updates
    * Flexmark to 0.50.50
    * Selenide to 5.7.0
    * Tomcat to 8.5.51

**2002-02-14  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M7-git-07_

* AttachmentManager:  fix the order of processing.  Added a few extra unit tests.

* Few minor SonarCloud fixes

**2020-01-28  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-06_

* [JSPWIKI-120](https://issues.apache.org/jira/browse/JSPWIKI-120): Separate rendering engine from core
    * `CommandResolver` renamed as `org.apache.wiki.ui.DefaultCommandResolver`, with new
       `org.apache.wiki.ui.CommandResolver` extracted as interface of the latter
    * `ProgressManager` renamed as `org.apache.wiki.ui.progress.DefaultProgressManager`, with new
      `org.apache.wiki.ui.progress.ProgressManager` extracted as interface of the latter
    * removed `createContext(..)` method from `WikiEngine` use new constructor on `WikiContext` instead
    * removed `WikiEngine#getRedirectURL(..)` use same method on `WikiContext`
* Removed `WikiEventUtils` if relying on it use directly `WikiEventManager.getInstance().addWikiEventListener( client, listener )`
* Fixed possible synchronization issues on `DefaultProgressManager` and `WikiAjaxDispatcherServlet`
* `PageEventFilter` moved from `event` to `filters` package
* `WikiEngine#init(..)` now enforces at least a 3.1 servlet api environment, inline with the servlet-api dependency version
* [JSPWIKI-1127](https://issues.apache.org/jira/browse/JSPWIKI-1127): Get rid of `jspwiki.referenceStyle`
* Dependency updates
    * ASF parent pom to version 23
    * Awaitility to 4.0.2
    * Flexmark to 0.50.48
    * JUnit to 5.6.0
    * Lucene to 8.4.1
    * SLF4J to 1.7.30

**2020-01-16  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-05_

* [JSPWIKI-120](https://issues.apache.org/jira/browse/JSPWIKI-120): Separate rendering engine from core
    * `RenderingManager` renamed as `org.apache.wiki.render.DefaultRenderingManager`, with new
       `org.apache.wiki.render.RenderingManager` extracted as interface of the latter
    * moved `textToHtml(..)` methods from `WikiEngine` to `RenderingManager`
    * moved `getHTML(..)` methods from `WikiEngine` to `RenderingManager`
    * moved `beautifyTitle( String )` and `beautifyTitleNoBreak` methods from `WikiEngine` to `RenderingManager`
* `VAR_EXECUTE_PLUGINS` and `WYSIWYG_EDITOR_MODE` constants from `RenderingManager` moved to `WikiContext` (the latter
as `VAR_WYSIWYG_EDITOR_MODE`)
* constant `PROP_RUNFILTERS` from `WikiEngine` moved to `VariableManager` as `VAR_RUNFILTERS`.
* constants `PUNCTUATION_CHARS_ALLOWED` and `LEGACY_CHARS_ALLOWED` from `MarkupParser` moved to `TextUtil`.
* Update ASF parent pom + plugin definitions to support, as far as possible, [reproducible builds](https://maven.apache.org/guides/mini/guide-reproducible-builds.html)

**2020-01-09  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-04_

* [JSPWIKI-120](https://issues.apache.org/jira/browse/JSPWIKI-120): Separate rendering engine from core - following methods moved from
`WikiEngine` to `PageManager`
    * `deletePage(..)` and `deleteVersion(..)` methods
    * `getPage(..)`
    * `pageExist(..)` methods, renamed as `wikiPageExist(..)`
    * `saveText()`
    * `getText(..)` methods
    * `getPureText( String, int )`
    * `getRecentChanges()`
    * `getVersionHistory()`
    * `getCurrentProvider()`
    * `getCurrentProviderInfo()` and `getPageCount` were deleted - use instead existing `getProviderDescription()` and `getTotalPageCount`
    methods from `PageManager`
* `WorkflowManager` renamed as `org.apache.wiki.workflow.DefaultWorkflowManager`, with new
 `org.apache.wiki.workflow.WorkflowManager` extracted as interface of the latter
* Explicit casting from `WikiEngine#getAttribute()` and ` WikiPage#getAttribute()` no longer needed in most cases now
* `WikiTagBase#ATTR_CONTEXT` constant moved to `WikiContext`
* `TestEngine` now includes test class and method where it was created next to the timestamp, which is useful when you have tons of
timestamped dirs and want to know which folder was generated by what test
    * Also, if no folder is given for work, pages and attachment dirs, it tries to place them under `./target/`
* Applied format & fixes suggested by intellij to another bunch of files

**2020-01-03  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-03_

* [JSPWIKI-120](https://issues.apache.org/jira/browse/JSPWIKI-120): Separate rendering engine from core
    * `ReferenceManager` renamed + moved to `org.apache.wiki.references.DefaultReferenceManager`, with new
    `org.apache.wiki.references.ReferenceManager` extracted as interface of the latter
    * `scanWikiLinks(..)` and `updateReferences(..)` methods from `WikiEngine` moved to `ReferenceManager`
* `WikiDifferenceManager`, `WikiVariableManager` and `WikiPageRenamer` renamed to `DefaultDifferenceManager`, `DefaultVariableManager`
  and `DefaultPageRenamer` respectively; it's a better suited prefix for default implementations and also follows the existing naming
  with the existing bunch of `Default[XYZ]Manager` that currently exist
* Moved `[Default]VariableManager` to their own package under `org.apache.wiki.variables`
* Dependency updates
    * Flexmark to 0.50.46
    * Lucene to 8.4.0
    * Selenide to 5.6.0
    * Tomcat to 8.5.50
* Applied format & fixes suggested by intellij to a big bunch of files

**2019-12-20  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-02_

* [JSPWIKI-120](https://issues.apache.org/jira/browse/JSPWIKI-120): Separate rendering engine from core
    * `DifferenceManager` renamed as `WikiDifferenceManager`, with new `DifferenceManager` extracted as interface of `WikiDifferenceManager`
    * `getDiff(..)` method deleted from `WikiEngine`, use the one located on `DifferenceManager`
    * `VariableManager` renamed as `WikiVariableManager`, with new `VariableManager` extracted as interface of `WikiVariableManager`
    * `getVariable(..)` method deleted from `WikiEngine`, use the one located on `VariableManager`

**2019-12-19  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M7-git-01_

* [JSPWIKI-120](https://issues.apache.org/jira/browse/JSPWIKI-120): Separate rendering engine from core
    * `PageRenamer` renamed as `WikiPageRenamer`, with new `PageRenamer` extracted as interface of `WikiPageRenamer`
    * `pageRename(..)` method deleted from `WikiEngine`, use the one located on `PageRenamer`
    * custom `PageRenamer`s should also fire the appropiate `WikiPageRenameEvent` on `pageRename(..)` method

**2019-12-06  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M6-git-05_

* Couple of methods moved out of `WikiEngine`
    * `safeGetQueryParameter`: moved as a static method on `HttpUtil`; it now requires content encoding as a parameter
    * `getRequiredProperty`: moved back as a static method to `TextUtil` - it now throws a `NoSuchElementException` (unchecked) instead of `NoRequiredPropertyException` (checked)

* Maven plugins changes
    * Change javadocs' doclet to [UMLDoclet](https://github.com/talsma-ict/umldoclet), which can be used with JDK >= 9 (see associated note on [`mvn_cheat-sheet.md`](https://github.com/apache/jspwiki/blob/master/mvn_cheat-sheet.md#3-reports-specific) for details)
    * Set `compilerVersion` to `jdk.version` on jspc-maven-plugin

* [JSPWIKI-1126](https://issues.apache.org/jira/browse/JSPWIKI-1126): Dependency updates
    * Flexmark to 0.50.44
    * Lucene to 8.3.1
    * Selenide to 5.5.1
    * SLF4J to 1.7.29
    * Tika to 1.23
    * Tomcat to 8.5.49

**2019-11-05  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M6-git-04_

* [JSPWIKI-1120](https://issues.apache.org/jira/browse/JSPWIKI-1120): Strings and Boxed types should be compared using "equals()".
  Contributed by Haris Adzemovic, thanks!

**2019-11-01  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M6-git-03_

* [JSPWIKI-1125](https://issues.apache.org/jira/browse/JSPWIKI-1125): Bringing in Docker support to ASF repo from https://github.com/metskem/docker-jspwiki.
  See https://jspwiki-wiki.apache.org/Wiki.jsp?page=Docker for details.

* Dependency updates
    * Flexmark to 0.50.42
    * Selenide to 5.5.0
    * Tomcat to 8.5.47

**2019-10-12  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M6-git-02_

* `FileUtils.copyContents( InputStream, OutputStream )` enforces writes to disk for `FileOutputStream` through
  their associated `FileDescriptor`.

* [JSPWIKI-1124](https://issues.apache.org/jira/browse/JSPWIKI-1124): `TestEngine` improvements
    * new static methods to build `TestEngine` instances which do not throw checked Exceptions and thus allows instances to be built as member of test classes, instead of rebuilding for every test, saving some time in the process.
    * `TestEngine` will generate separate page, attachment and work directories, in order to allow each instance
  to work with a clean file installation.

**2019-10-10  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M6-git-01_

* Introduce [Awaitility](https://github.com/awaitility/awaitility) to speed up tests formerly relaying on `Thread.sleep(..)`

* `LuceneSearchProvider` now uses an `Executor` to increase performance on searches.

**2019-09-12  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-19_

* Fix javascript build error,  and some favicon errors.

**2019-09-10  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M5-git-18_

* [JSPWIKI-1115](https://issues.apache.org/jira/browse/JSPWIKI-1115): a few more dependency upgrades before 2.11.0-M5
    * Commons Text to 1.8
    * Flexmark to 0.50.40
    * Hsqldb to 2.5.0
    * JUnit to 5.5.2
    * Lucene to 8.2.0
    * Selenide to 5.3.1

**2019-09-07  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-17_

* Few critical sonarcloud fixes; added clean parsing of `skin` parameter

**2019-08-31  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-16_

* Fixed `InfoContent.jsp` vulnerability in old jspwiki template (templates/211/...)
  related to the rename parameter.

* Fixed `preview.jsp` vulnerability related to the remember parameter.


**2019-08-31  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-15_

* Improve UI accessibility (ref. sonarcloud report)

**2019-08-27  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-14_

* XSS vulnerability on the page rename parameter

* Few sonarcloud fixes

**2019-08-24 Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-13_

* Various small fixes in html & jsp files, reported by sonarcloud.

**2019-08-24 Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M5-git-12_

* [JSPWIKI-1115](https://issues.apache.org/jira/browse/JSPWIKI-1115): Upgrade flexmark to 0.50.28 and tomcat to 8.5.45

* Ended up removing/replacing all `@Deprecated` code

**2019-08-21  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-11_

* [JSPWIKI-1097](https://issues.apache.org/jira/browse/JSPWIKI-1097) Various smaller JS refactorings, and bugfixes.

* Clean up several minor JSP bugs reported by sonarcloud.

**2019-08-19  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M5-git-10_

* Removed `@Deprecated` code. A complete analysis of what have changed will be available at
  http://jspwiki.apache.org/japicmp/index.html once 2.11.0.M5 gets released

* [INFRA-18845](https://issues.apache.org/jira/browse/INFRA-18845): switch Sonar instance to sonarcloud.io

**2019-08-16  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M5-git-09_

* [JSPWIKI-893](https://issues.apache.org/jira/browse/JSPWIKI-893): Cannot search for bold words with `GermanAnalyzer`

* replaced all deprecated code

* [JSPWIKI-1115](https://issues.apache.org/jira/browse/JSPWIKI-1115): Upgrade SLF4J to 1.7.28 and commons-lang from 2.6 to 3.9
    * dev-only breaking change: if you were using commons-lang transitively on your extension,
    you must declare it explicitly or migrate it to commons-lang 3

**2019-08-06  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M5-git-08_

* [JSPWIKI-427](https://issues.apache.org/jira/browse/JSPWIKI-427): Keywords for Lucene Index

* [JSPWIKI-1114](https://issues.apache.org/jira/browse/JSPWIKI-1114): Show only part of Weblog entry on the overview page. Contributed by
  Ulf Dittmer, thanks!

* [JSPWIKI-1115](https://issues.apache.org/jira/browse/JSPWIKI-1115): Upgrade bundled dependencies for 2.11.0.M5
    * Flexmark 0.50.26
    * JUnit 5.5.1
    * Lucene 8.1.1
    * Selenide 5.2.8
    * Tika 1.22
    * Tomcat 8.5.43

**2019-16-07  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-07_

* [JSPWIKI-1097](https://issues.apache.org/jira/browse/JSPWIKI-1097) Various smaller JS refactorings, and bugfixes.

* Trim spaces from the rendered html to reduce page weight.

**2019-09-07  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-06_

* [JSPWIKI-427](https://issues.apache.org/jira/browse/JSPWIKI-427): Adding keyword support for JSPWiki pages.
  Use `[{SET keywords=a,b,c}]` to add keywords to a page.
  They will be shown in the info drop-down menu, and are added as
  `META` tags to your page.

* Remove XSS vulnerability on the plain editor section drop-down

* [JSPWIKI-1097](https://issues.apache.org/jira/browse/JSPWIKI-1097) Various smaller JS refactorings


**2019-04-07  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-05_

* Adapt to JAVA EE 7 XMLS scheme namespace to support container authentication.
  Minimum requirement since 2.11.0-M1 is JSP Servlet 3.1.
  (testcases with older `web.xml` still to be updated)


**2019-06-15  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-04_

* [JSPWIKI-1097](https://issues.apache.org/jira/browse/JSPWIKI-1097) Refactored `%%collapse` and `%%collapsebox`.
  Added keyboard support to expand/collapse lists and boxes. Bugfixes on cookie handling.


**2019-05-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-03_

 * [JSPWIKI-1112](https://issues.apache.org/jira/browse/JSPWIKI-1112) EDITOR input fields (changenote,comment-signature )
   vulnerable to XSS.

* [JSPWIKI-1097](https://issues.apache.org/jira/browse/JSPWIKI-1097) Minor JS updates (cookie handling, %%collapse)


**2019-05-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-02_

 * Improved styling of the WYSIWYG editor toolbar

* [JSPWIKI-1111](https://issues.apache.org/jira/browse/JSPWIKI-1111) Improve handling of `&entities;` in the WYSIWYG editor.

* Improve the web app manifest making JSPWiki a progressive web app.
  You can now install JSPWIKI on the homescreen of your mobile device or tablet
  for quicker access and improve experience.

* [JSPWIKI-1097](https://issues.apache.org/jira/browse/JSPWIKI-1097) JS updates to start replacing `mootools.js`  (cookie handling)


**2019-05-17  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M5-git-01_

* Accessibility improvements (ref. https://accessibilityinsights.io)

**2019-05-11  Juan Pablo Santos (juanpablo AT apache DOT org)**

* prepare release for 2.11.0.M4

**2019-05-02  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M4-git-13_

* [JSPWIKI-469](https://issues.apache.org/jira/browse/JSPWIKI-469) new [TikaSearchProvider](https://jspwiki-wiki.apache.org/Wiki.jsp?page=TikaSearchProvider) to index a lot more of attachments. It is not bundled
by default, as it brings in a lot of dependencies (+55MB).
    * See [TikaSearchProvider](https://jspwiki-wiki.apache.org/Wiki.jsp?page=TikaSearchProvider) for installation instructions
    * Search provider contributed by Ulf Dittmer, thanks!

* `LuceneSearchProvider` now indexes all attachment filenames, whether their content is parsed or not,
and also scans `.md` and `.xml` files.

* Updated missing es translations


**2019-05-01  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-12_

* [JSPWIKI-1104](https://issues.apache.org/jira/browse/JSPWIKI-1104) [InsertPagePlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=InsertPagePlugin)
  now also supports cookie based inserts.
  The UserPreferences page has been extended to allow the users to view and
  delete page-based cookies.


**2019-04-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-11_

* [JSPWIKI-1107](https://issues.apache.org/jira/browse/JSPWIKI-1107) Fixing XSS vulnerability in the navigation breadcrumbs (Trail link)

* Small ui improvement: make Attachment lists sortable on the attachment size field


**2019-04-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-10_

* [JSPWIKI-1107](https://issues.apache.org/jira/browse/JSPWIKI-1107), [JSPWIKI-1109](https://issues.apache.org/jira/browse/JSPWIKI-1109) Fixing XSS vulnerability in various plugins.

* [JSPWIKI-1106](https://issues.apache.org/jira/browse/JSPWIKI-1106) Adding the `jspwiki.attachment.forceDownload` property


**2019-04-28  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M4-git-09_

* [JSPWIKI-1110](https://issues.apache.org/jira/browse/JSPWIKI-1110) Upgrade bundled dependencies for 2.11.0.M4

* generate aggregated javadocs for http://jspwiki.apache.org/apidocs/index.html

**2019-04-27  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-08_

* [JSPWIKI-1107](https://issues.apache.org/jira/browse/JSPWIKI-1107) fixing file-type vulnerability

* Fixing consistency of the sidebar collapse in `Upload.jsp`

**2019-04-25  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-07_

* [JSPWIKI-1107](https://issues.apache.org/jira/browse/JSPWIKI-1107) uploading attachments with illegal filename causes XSS vulnerability
  Fixing file upload vulnerability.

**2019-04-23  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-06_

* [JSPWIKI-1109](https://issues.apache.org/jira/browse/JSPWIKI-1109) [ReferredPagesPlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=ReferredPagesPlugin) with illegal characters in parameters
  causes XSS vulnerability

**2019-04-23  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-05_

* [JSPWIKI-1108](https://issues.apache.org/jira/browse/JSPWIKI-1108) interwiki links with illegal characters causes XSS vulnerability

* [JSPWIKI-1107](https://issues.apache.org/jira/browse/JSPWIKI-1107) uploading attachments with illegal filename causes XSS vulnerability
  Fixing side effect on slimbox links, when rendering the caption with illegal characters.

**2019-04-22  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-04_

* [JSPWIKI-1107](https://issues.apache.org/jira/browse/JSPWIKI-1107) uploading attachments with illegal filename causes XSS vulnerability

**2019-04-05  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-03_

* [JSPWIKI-1100](https://issues.apache.org/jira/browse/JSPWIKI-1100) Add support for mixed css-class & css-style markup

**2019-04-05  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-02_

* [JSPWIKI-1101](https://issues.apache.org/jira/browse/JSPWIKI-1101) Improve rendering of `{{{inline preformatted text}}}`

* Change UI for attachement upload: by default, the FILE SELECTION input should be visible

**2019-04-05  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M4-git-01_

* Added interwiki link for location links (google maps)
  eg: `[Atomium|Location:Atomium, City of Brussels, Belgium]`

* Added plain editor configs as regular properties.
  By default, the editor preview, autosuggestion and tabcompletion are set.

* Various small js / css tweaks.

* Add format option to `<wiki:Author/>` to render force rendering of text iso link

**2019-03-22  Juan Pablo Santos (juanpablo AT apache DOT org)**

* prepare release for 2.11.0.M3

**2019-03-21  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M3-git-04_

* [JSPWIKI-1095](https://issues.apache.org/jira/browse/JSPWIKI-1095): `DefaultURLConstructor#getForwardPage( HttpServletRequest req )` now always returns `Wiki.jsp`

* [JSPWIKI-1096](https://issues.apache.org/jira/browse/JSPWIKI-1096): Upgrade bundled dependencies for 2.11.0-M3
    * flexmark to 0.40.24
    * lucene to 8.0.0
    * selenide to 5.2.2
    * slf4j to 1.7.26
    * tomcat to 8.5.39

**2019-03-19  Juan Pablo Santos (juanpablo AT apache DOT org)**

* [JSPWIKI-1094](https://issues.apache.org/jira/browse/JSPWIKI-1094): `mvn eclipse:eclipse` fails, patch provided by Christian Fröhler, thanks! (no version bump)

**2019-03-17  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M3-git-03_

* Remove unused top level JSP. See also [JSPWIKI-1093](https://issues.apache.org/jira/browse/JSPWIKI-1093)

**2019-03-09  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M3-git-02_

* Adding Drag & Drop capabilities to the standard JSPWiki editor:
  links automatically are converted to `[description|url]` format
  other content (text, tables, etc...) will be converted to wiki-markup.


**2019-03-09  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M3-git-01_

* UserPreferences fixes
    * new toggle style for on/off switches
    * restyle bi-model checkboxes for Page Layout setting
    * fix a few style issues in the UserPreferences when in Dark Mode

* The JSPWiki template should by default open up in the light mode.

* Consistent css-style for the toolbar of the jspwiki wysiwyg editor

* Added `Description` meta-tag to improve on-page SEO

* Added the `Content-Security-Policy` meta-tag to reduce the risk of XSS attacks.


**2019-03-04  Juan Pablo Santos (juanpablo AT apache DOT org)**

* prepare release for 2.11.0.M2

**2019-02-21  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M2-git-10_

* `o.a.jwpiki.util` package moved to its own submodule

* remove `DefaultAclManager` dependency from `WikiEngine` and other small refactors

**2019-02-15  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M2-git-09_

* Clean browsers LocalStorage cache of unsaved page edits
  when switching between plain & WYSIWYG editors.

* Fix (resizable) height of the wysiwig editor HTML viewer.


**2019-02-15  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M2-git-08_

* WYSIWYG editor was still pointing to the Haddock template.


**2019-02-15  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M2-git-07_

* [JSPWIKI-1088](https://issues.apache.org/jira/browse/JSPWIKI-1088): Fallback to default template if `jspwiki.templateDir` if the requested template folder is
  not found

* [JSPWIKI-1092](https://issues.apache.org/jira/browse/JSPWIKI-1092): Upgrade bundled dependencies
    * flexmark to 0.40.16
    * gson to 2.8.5
    * lucene to 7.7.0
    * nekohtml to 1.9.22
    * stripes to 1.7.0-async (needed to test [JSPWIKI-1088](https://issues.apache.org/jira/browse/JSPWIKI-1088))
    * tomcat to 8.5.38

* fixed css path and packaging of webresources

* updated both new and missing es resources

**2019-02-14  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M2-git-06_

* lots of internal refactorings, mostly around a) Task's implementations extracted to their own package
  and b) `PageManager` and `PageLock` moved to `o.a.w.pages` package, in order to untangle some class/package
  circular dependencies

* JUnit updated to 5.4.0

**2019-02-13  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M2-git-05_

* Adding DARK template to the User Preference.


**2019-02-13  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M2-git-04_

* [JSPWIKI-1091](https://issues.apache.org/jira/browse/JSPWIKI-1091): Broken DIFF view
  Added some missing JSP in default template. (caused by template rename)

* Added missing `<html lang=..>` localization


**2019-02-08  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M2-git-03_

* merged [PR#10](https://github.com/apache/jspwiki/pull/10): `DOCTYPE` and `HTML lang="en"` attribute addition
  (thanks to Scott Fredrickson)

* `o.a.w.util` package ready to be extracted to its own module

* new `o.a.w.pages` package, with `PageSorter` becoming a `Comparator< String >` and no longer accesible from WikiContext
    * `PageSorter` is accesed now through `PageManager`
    * to compare WikiPages use `wikiPage.compareTo( anotherWikiPage );`
    * `sortPages` methods are also gone, as an alternative you can use something along these lines (see
    `AttachmentManager#listAttachments` for another example):
    `Collections.< WikiPage >sort( pages, Comparator.comparing( WikiPage::getName, m_engine.getManager( PageManager.class ).getPageSorter() ) );`
    * as a side effect of this change, `AbstractReferalPlugin#filter[AndSort]Collection` methods operate with
    `Collection< String >` instead of with plain `Collection` (except for `RecentChangesPlugin`, plugins
    inheriting this method were already doing it), custom plugins inheriting this method will have to use
    new `filterWikiPageCollection` method instead

* other internal code refactors

**2019-02-03  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M2-git-02_

* [JSPWIKI-1074](https://issues.apache.org/jira/browse/JSPWIKI-1074): Fixed buggy header width in Fixed Page Layout

**2019-02-03  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M2-git-01_

* [JSPWIKI-1090](https://issues.apache.org/jira/browse/JSPWIKI-1090): Fixed READER view, bug caused by the rename of the HADDOCK template

**2019-01-25  Juan Pablo Santos (juanpablo AT apache DOT org)**

* prepare release for 2.11.0.M1

**2019-01-20  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-15_: [JSPWIKI-1086](https://issues.apache.org/jira/browse/JSPWIKI-1086) - selenide-based functional tests infrastructure + login/logout
  associated tests (selenium tests currently kept for reference)

* [JSPWIKI-1085](https://issues.apache.org/jira/browse/JSPWIKI-1085) - JSPWiki locale can be set server-side - to determine the locale used, the following order is used:
  - user-preference settings
  - if not set, see if there is a locale set server-side, as noted by
      `jspwiki.preferences.default-locale` setting on `jspwiki[-custom].properties`
  - if not set, browser's preferred language setting
  - if not set, JVM's default

* [JSPWIKI-1087](https://issues.apache.org/jira/browse/JSPWIKI-1087) - upgrade bundled dependencies
    * commons-fileupload to 1.4
    * flexmark to 0.40.12
    * hsqldb updated to 2.4.1
    * cargo plugin to 1.7.1

**2018-12-25  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-14_: upgrade bundled dependencies
    * commons-fileupload to 1.3.3
    * ehcache to 2.10.6
    * flexmark to 0.35.0
    * junit to 5.3.2
    * lucene to 7.6.0
    * tomcat to 8.5.37

* escape entities on `Captcha.jsp` request parameters

* [JSPWIKI-1084](https://issues.apache.org/jira/browse/JSPWIKI-1084) - `Jenkinsfile` now builds both source and website, `jspwiki-site` job can still be triggered manually

* few more polishing and minor refactors

**2018-12-24  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M1-git-13_: AJAX based search results are not shown.
  Fixing `AJAXSearch.jsp`.
  The java `<>` diamond operator is not allowed for source level below 1.7.

**2018-12-15  Dirk Frederickx (brushed AT apache DOT org)**

* _2.11.0-M1-git-12_: [JSPWIKI-1083](https://issues.apache.org/jira/browse/JSPWIKI-1083) - fixing bugs related to the new default template.
    * Rename HADDOCK template to DEFAULT template.
    * Moving a number of default jsp's (common for all templates)
    from templates/210 to templates/default.

**2018-12-07  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-11_: [JSPWIKI-1082](https://issues.apache.org/jira/browse/JSPWIKI-1082) - fix from 2.11.0.M1-rc1 - revert change from commit
  `87bf9b941fdf` (Nov/11/2018) that ended up causing lots of `ClassCastException`

**2018-12-03  Juan Pablo Santos (juanpablo AT apache DOT org)**

* prepare release for 2.11.0.M1

**2018-11-20  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-10_: backwards-incompatible change - move `TextUtil.getRequiredProperty` to `WikiEngine`

* some more polishing and minor refactors

**2018-11-13  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-09_: fix JSP compilation error; added jspc plugin to ensure JSPs remain well-formed

* JSPWiki's custom tags TLD moved to main module

* some more polishing and minor refactors

**2018-11-11  Juan Pablo Santos (juanpablo AT apache DOT org)**

* no version bump: some more polishing and minor refactors

**2018-11-05  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-08_: [JSPWIKI-1080](https://issues.apache.org/jira/browse/JSPWIKI-1080) try to load as many external third-party plugin jars as possible,
  instead of all-or-nothing

* small backwards-incompatible changes:
    * `CryptoUtil#verifySaltedPassword` doesn't throw `UnsupportedEncodingException` anymore
    * `TextUtil#urlDecode` methods don't throw `UnsupportedOperationException` anymore
    * `ClassUtil#getMappedObject` methods now throw `ReflectiveOperationException`, `IllegalArgumentException`
    instead of `WikiException`
    * `ClassUtil#getMappedClass` method now throws `ClassNotFoundException` instead of `WikiException`

* fix possible concurrency issue at `ReferenceManager#serializeAttrsToDisk`

* remove commons-codec usage from source (it's carried onto the war as a transitive
  dependency though), plus several other small refactors

**2018-11-02  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-07_: removed a bunch of warnings throughout the code

* added a `Jenkinsfile`

* small backwards-incompatible change: `WikiEngine#getRecentChanges()` now returns a `Set` instead of a `Collection`

**2018-10-30  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-06_: fix JSPs using methods removed on 2.11.0-M1-git-05

**2018-10-29  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-05_: [JSPWIKI-1081](https://issues.apache.org/jira/browse/JSPWIKI-1081) - maven build refactor
  - java code split into its own module: custom extensions should now rely on
    the new `org.apache.jspwiki:jspwiki-main` dependency, instead of the
    `org.apache.jspwiki:jspwiki-war:classes` old one
  - `parent-pom`: clean-up + dependencies and plugins versions' set as maven properties
  - `jspwiki-markdown` module included into main war

* several small code refactors, including some backwards-incompatible ones:
    * `PropertiesUtils` moved to the `util` package
    * `ReferenceManager#findReferrers` returns `Set< String >` instead of `Collection< String >`
    * `AttachmentManager#listAttachments` returns a `List< Attachment >` instead of a `Collection`
    * `WikiEngine#findPages( String query, WikiContext wikiContext )` is removed, use
    `WikiEngine#getSearchManager()#findPages( String query, WikiContext wikiContext )` instead

**2018-10-26  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-04_: [JSPWIKI-1078](https://issues.apache.org/jira/browse/JSPWIKI-1078) - update tests to JUnit 5.3.1

* updated versions of maven plugins

* flexmark updated to 0.34.56

**2018-10-21  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-03_: [JSPWIKI-1083](https://issues.apache.org/jira/browse/JSPWIKI-1083) - Haddock is now the default template
    * to bring back the 2.10 template set the `jspwiki.templateDir` property to `210`

* [JSPWIKI-1077](https://issues.apache.org/jira/browse/JSPWIKI-1077) - added the following pages to the core pages' bundles, as they enable some more haddock functionality
    * [CSSBackgroundGradients](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Background%20Gradients)
    * [CSSBackgroundPatterns](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Background%20Patterns)
    * [CSSInstagramFilters](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Instagram%20Filters)
    * [CSSPrettifyThemeTomorrowNightBlue](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Prettify%20Theme%20Tomorrow%20Night%20Blue)
    * [CSSPrettifyThemeTomorrowPrism](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Prettify%20Theme%20Prism)
    * [CSSRibbon](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Ribbon)
    * [CSSStripedText](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Striped%20Text)
    * [CSSThemeCleanBlue](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Clean%20Blue%20Theme)
    * [CSSThemeDark](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Dark%20Theme)

**2018-09-30  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-02_: [JSPWIKI-1076](https://issues.apache.org/jira/browse/JSPWIKI-1076) - minimum required Servlet / JSP is now 3.1 / 2.3

* [JSPWIKI-1079](https://issues.apache.org/jira/browse/JSPWIKI-1079) - add `jspwiki-markdown` to the main build

* flexmark updated to 0.34.46

**2018-09-14  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.11.0-M1-git-01_: [JSPWIKI-1076](https://issues.apache.org/jira/browse/JSPWIKI-1076) - minimum required to build/run is now Java 8 / Maven 3.5.0

* fix for [JSPWIKI-932](https://issues.apache.org/jira/browse/JSPWIKI-932) - Failed to start managers. `java.util.ConcurrentModificationException`

**2018-08-31  Juan Pablo Santos (juanpablo AT apache DOT org)**

* update flexmark to 0.34.22 and ASF parent pom to 21

* prepare release for 2.10.5

**2018-08-31  Siegfried Goeschl (sgoeschl@apache.org)**

* _2.10.5-git-09_: [JSPWIKI-1073](https://issues.apache.org/jira/browse/JSPWIKI-1073) Upgrade the `jspwiki-portable` build

**2018-07-08  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.5-git-08_: update flexmark to 0.34.6 and slf4j to 1.7.25

**2018-07-09  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.5-git-07_: fix to plain haddock editor related to the new
  functionality to recover unsaved page edits.

  You can test this like so:
    * open a page for edit in the haddock template
    * make some changes to the page
    * move to another page without saving (or close the browser tab)
    * click LEAVE when the popup `changes you made may not be saved` appears.
    * reopen the page for edit
    * you should now receive a new popup which allows you to restore or abandon
    the unsaved changes


**2018-07-08  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.5-git-06_ : update bundled Apache Tomcat on portable JSPWiki to 7.0.90

**2018-07-01  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.5-git-05_ : few more haddock template updates

* The haddock editor has now a page restore functionality to recover unsaved page
  edits.  When your session or login timer expires, or your accidentally close a
  browser tab without saving, etc...  your last changes are now preserved in the
  localstorage area of your browser.
  At the start of a new edit sessions,  you will be presented with a modal dialog
  to restore the cached page.

* The "attach" and the "info" menu are now combined into a single navigation menu.
  The INFO or ATTACHMENT UPLOAD page can be accessed with an additional
  click of a button. This also improves usability on touch devices.

* Small refinements of the Search and User buttons (top-right corner)
  to improve support for touch devices. (issue reported by Juan Pablo)


**2018-06-17  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.5-git-04_  Fine-tuning Haddock for mobile devices.

* Improve accessibility on hover-menu's for touch-devices. They'll open after
  a touch-down event; and will close when touching any area outside the menu.

* Fixing access to the Find and User menu's for touch devices.

* Several small style improvements on the navigation bar for mobile devices
  (hiding CARET to indicate hover-menu's, ...)

* Added touch-support for `%%magnify` style.

* Breadcrumbs are now moved to a proper drop-down menu (...) on the navigation bar
  instead of the previously not-so-obvious 'mouse-over-zone' under the pagename.
  This also makes breadcrumbs accessible to the tablet & phone users.

* Fixed a display error when uploading multiple attachements in one step.


**2018-06-05  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.5-git-03_  [JSPWIKI-1071](https://issues.apache.org/jira/browse/JSPWIKI-1071) Ajax request header 'Connection' forbidden
  impacting the DEFAULT jspwiki template.


**2018-06-03  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.5-git-02_

* [JSPWIKI-1070](https://issues.apache.org/jira/browse/JSPWIKI-1070): (properly) Support JDK 10 builds

* Generate sha1 and sha512 checksums for build artifacts

**2018-05-27  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.5-git-01_

* [JSPWIKI-1070](https://issues.apache.org/jira/browse/JSPWIKI-1070): Support JDK 10 builds

**2018-04-29  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.4-git-10_  Bugfix in `AddCSS.JS` related to url() parsing

**2018-04-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.4-git-09_  Better support for mobile devices in the Haddock Template
     See [JSPWIKI-835](https://issues.apache.org/jira/browse/JSPWIKI-835)
       * Updates various styles to better fit small screens.  (tabs, accordion, columns, ...)
       * On small screens,  the sidebar is by default closed.
On wider screens, the sidebar is open/closed based on the previous state
which is saved in a cookie.

**2018-04-22  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.4-git-08_  Adding support for mobile devices to the Haddock Template
     See [JSPWIKI-835](https://issues.apache.org/jira/browse/JSPWIKI-835)
     Part-1 -- many style adjustment to fit smaller screens
       * Sidebar now slides over the main page on mobile devices
       * Header (pagename, and menu bar) are better fit for small screens
       * Width of several menu's and dropdowns is restricted for small screens
       * Editor toolbar resized for small screens

     Other:
       * [JSPWIKI-1058](https://issues.apache.org/jira/browse/JSPWIKI-1058) Editor toolbar now remains on screen, even when scrolling down
       * Small tweaks of the `RecentChanges` output
       * [JSPWIKI-1068](https://issues.apache.org/jira/browse/JSPWIKI-1068) : fixing positioning of the `TitleBox`


**2018-04-19  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.4-git-07_ Adding a favicon to the haddock template

**2018-04-11  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.4-git-06_  [JSPWIKI-1069](https://issues.apache.org/jira/browse/JSPWIKI-1069) i18n errors in german translation

**2018-03-31  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.4-git-05_  [JSPWIKI-1068](https://issues.apache.org/jira/browse/JSPWIKI-1068) `TitleBox` rendering on Haddock

**2018-03-29  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.4-git-04_

* [JSPWIKI-1039](https://issues.apache.org/jira/browse/JSPWIKI-1039) / [JSPWIKI-1067](https://issues.apache.org/jira/browse/JSPWIKI-1067): ACLs are not taken into account when cache
  is disabled / View-only ACLs are not enforced

**2018-03-29  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.4-git-03_

* Main page can be revealed when invoking some JSPs without parameters
  (reported by Motohiko Matsuda, thanks!)

**2018-03-25  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.4-git-02_

* Further fix on [JSPWIKI-1064](https://issues.apache.org/jira/browse/JSPWIKI-1064) - Link to non-existing page doesn't change if
  linked page is created, not all page caches were properly flushed.

**2018-03-04  Juan Pablo Santos (juanpablo AT apache DOT org)**

* Fixed all javadoc errors when building using java 8 - no version bump

**2018-02-25  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.4-git-01_  Fixing Admin JSP Bugs

* Quick fix to admin and user management pages: adding proper tabs to
  the ADMIN page, fixing javascript bugs in user management page
  (reported by Harry)

**2018-02-03  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.3-git-48_

* [JSPWIKI-835](https://issues.apache.org/jira/browse/JSPWIKI-835) - better mobile experience: move sidebar to bottom on
  extra-small devices (< 768px, only on haddock template)

* Some internal refactors to `org.apache.wiki.WatchDog`

* Flexmark updated to 0.28.38.

**2018-01-27  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.3-git-47_

* Another fix on [JSPWIKI-1064](https://issues.apache.org/jira/browse/JSPWIKI-1064) - Link to non-existing page doesn't change if
  linked page is created, as the page render cache must also take into account
  if the plugins should be rendered or not.

* JSPWiki portable: Update bundled Apache Tomcat to the latest version on 7.x branch
  and launch4j to 3.11.

* JSPWiki portable: As appbundler is not longer accesible through java.net, use
  fork at https://github.com/teras/appbundler instead.

* Updated maven plugins' versions to latest + use latest ASF parent pom.

**2018-01-21  Juan Pablo Santos (juanpablo AT apache DOT org)**

* Updated `<scm/>` section from main `pom.xml` so it points to github repo

* Flexmark updated to 0.28.34 (no version bump).

**2017-12-30  Juan Pablo Santos (juanpablo AT apache DOT org)**

* Upgraded all test from JUnit 3 to JUnit 4 (no version bump).

**2017-12-27  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.3-git-46_

* [JSPWIKI-802](https://issues.apache.org/jira/browse/JSPWIKI-802) - Markdown support
    * urls are not set on attribute provider derived classes, as this has some
    unwanted side effects. Introduced `JSPWikiLink`, a wrapper around Flexmark's
    links which retain the original wiki link.
    * updated Flexmark to 0.28.24.

**2017-12-16  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.3-git-45_

* Fixed [JSPWIKI-1064](https://issues.apache.org/jira/browse/JSPWIKI-1064) - Link to non-existing page doesn't change if linked page
  is created

* Improvement on [JSPWIKI-843 - exclude tests from `test-jar`

**2017-12-08  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.3-git-44_

* [JSPWIKI-802](https://issues.apache.org/jira/browse/JSPWIKI-802) - initial [markdown support](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Markdown%20Support)

**2017-12-03  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.3-git-43_

* Fixed [JSPWIKI-843](https://issues.apache.org/jira/browse/JSPWIKI-843) - generate `test-jar` for `jspwiki-war` (wasn't being generated)

* Extract `WikiLink` parsing operations from `JSPWikiMarkupParser`, `LinkParser`,
  `VariableManager` to their own class, `LinkParsingOperations`

* Move `(private) JSPWikiMarkupParser#getLocalBooleanProperty` to
 `(public) WikiContext#getBooleanWikiProperty`

**2017-11-16  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.3-git-42_

* small refactor to move some private constants to public at `MarkupParser` and
  `WikiRenderer`, so they can be reused throughout the code and custom extensions.

**2017-08-22  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.3-git-41_

* `WysiwygEditingRenderer` isn't hardcoded in JSPs anymore, and can be substituted
  through `jspwiki.renderingManager.renderer.wysiwyg` property on `jspwiki.properties`.
  This allows to develop custom renderers which do not expect the same information
  as the existing ones.

* Fixed `DefaultFilterManager#modules` not returning `Collection< WikiModuleInfo >`,
  as it was supposed to. This method wasn't used anywhere, until now, where it
  is used through FilterBean, a new JSPWiki AdminBean exposing existing filters
  information.

* `FilterBean` runs parallel to `PluginBean`, which allowed some more minor internal
  refactorings and code polishing.

* Moved some constants from `JSPWikiMarkupParser` to `MarkupParser`.

**2017-07-16  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.3-git-40_

* Some small changes around `JSPWikiMarkupParser`, needed to develop
  custom markup parsers, which do not rely on the former class or
  `WikiDocument`.

* Some other minor internal refactorings and code polishing

**2017-05-14  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-git-39_

* [JSPWIKI-1059](https://issues.apache.org/jira/browse/JSPWIKI-1059) - `ConcurrentModificationException` in `SessionMonitor`

**2017-04-22  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-git-38_  Haddock Template updates

* Fixing some minor issues with the Image plugin ALIGN parameter

* `%%magnify`: add a magnifying glass to reveal details of a cropped images.
  Hover the mouse above the image to see the effect.

* Redesigned Header
  The header (including the menu bar) now shifts up when you scroll down,
  and reappears when scrolling back-up.  So you can quickly have access
  to all the menu's and the quick search function.  Clicking the pagename
  in the header get's you immediately back to the top of the page.
  The menu bar has now become part of the (coloured) header section.

* Editor:
  Improved the speed of the editor quick preview when editing large pages.
  Updates to various editor auto-suggest dialogs.


**2017-03-18  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-git-37_  Haddock Template

* [JSPWIKI-1055](https://issues.apache.org/jira/browse/JSPWIKI-1055):  Haddock Special Block Marker
  Added a few icons to improve rendering of contextual blocks in B/W.

**2017-03-14  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-git-36_  Haddock Template

* Few fixes on the `%%column` style

**2017-03-12  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-git-35_  Haddock Template updates

* [ImagePlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=Image):  minor update to apply the css class and styles
  parameters to the image container, not to the whole table;
  escape HTML entities in captions.

* several CSS stylesheet additions
    * image styles: effects, captions, frames, animation
    * background styles: color, background images

* `%%columns`: bugfix, few more column styles

**2017-03-05  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-git-34_

* [JSPWIKI-1044](https://issues.apache.org/jira/browse/JSPWIKI-1044) - URL in password recovery mail is relative while it should be absolute

**2017-03-03  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-git-33_

* Fixed [JSPWIKI-1051](https://issues.apache.org/jira/browse/JSPWIKI-1051) - Startup fails due to `jspwiki.log (Permission denied)`

**2017-02-04  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-git-32_

* [JSPWIKI-1050](https://issues.apache.org/jira/browse/JSPWIKI-1050) The find and replace do not seem to work  (Haddock editor)

  Pressing Ctrl-F has been removed as short-cut key for the wiki editor.
  Ctrl-F brings you always to the standard browser dialog. (as expected)
  To open JSPWiki's Find&Replace dialog, click the toolbar button.

  The Find&Replace dialog now also indicates if text was selected before.
  If that case, the Find&Replace will run only on the selected text.


**2017-01-21  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-git-31_

* Fixed [JSPWIKI-1047](https://issues.apache.org/jira/browse/JSPWIKI-1047) - Access Control Lists do not work if page cache is deactivated (thanks to E. Poth)
* minor bugfix in [SessionsPlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=SessionsPlugin) (`StringIndexOutOfBoundsException` when using the `distinctUsers` option)

**2017-01-21  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-git-30_
  [JSPWIKI-1046](https://issues.apache.org/jira/browse/JSPWIKI-1046) IE11 detection fixed, txs to patch of Albrecht Striffler.


**2017-01-17  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-git-29_
  [JSPWIKI-1046](https://issues.apache.org/jira/browse/JSPWIKI-1046) IE11 scrolling in content page blocked.
  IE detection not working on IE11; ok on Edge. New detection method implemented.

**2017-01-15  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-git-28_
  [JSPWIKI-1045](https://issues.apache.org/jira/browse/JSPWIKI-1045) IE11 rendering is broken for `%%graphBars` using color names.

**2017-01-14  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-git-27_.

* Fixed [JSPWIKI-1042](https://issues.apache.org/jira/browse/JSPWIKI-1042) - Impossible to change user profile loginName, fullname, password (patch by Eric Kraußer)
* Fixed [JSPWIKI-1043](https://issues.apache.org/jira/browse/JSPWIKI-1043) - Encode email subjects as UTF-8 (patch by Eric Kraußer)

**2017-01-06  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-git-26_
  [JSPWIKI-1041](https://issues.apache.org/jira/browse/JSPWIKI-1041): fix some lines in `skin.css`

**2017-01-03  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-git-25_: few Haddock template fixes

* Remove the editor suggestion-dialogs scroll-bars (only visible on Windows)
  Fix a few formatting errors in sugestion dialogs.

* Fixed the width of the table filter input field

* Added console-logs to the editor for debugging on IE/EDGE
  (positioning of suggestion dialogs seems to be broken)

* Update JSON XmlHttpRequest header to avoid IE/EDGE XML5619 Document Syntax errors

**2016-12-31  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-git-24_.

* Fixed [JSPWIKI-1035](https://issues.apache.org/jira/browse/JSPWIKI-1035) - merged branch [JSPWIKI-1035 back to master

**2016-12-27  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-23_.

* Fix nesting of ul/li in [ReferringPagesPlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=ReferringPagesPlugin).

**2016-12-26  Harry Metske (metskem AT apache DOT org)**

* Fixed [JSPWIKI-1035](https://issues.apache.org/jira/browse/JSPWIKI-1035) - Get rid of `jspwiki.baseURL`
  fixed remaining unit tests
* changed Release postfix from `svn` to `git`

**2016-12-19  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-22_  Various HADDOCK updates & fixes.

* [JSPWIKI-1038](https://issues.apache.org/jira/browse/JSPWIKI-1038): Fix allowing flexbox in Edge. (but not in IE)


**2016-12-18  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-21_  Various HADDOCK updates & fixes.

* [JSPWIKI-1038](https://issues.apache.org/jira/browse/JSPWIKI-1038): IE's flexbox implementation is broken,
  no workaround for now.


**2016-12-17  Dirk Frederickx (brushed AT apache DOT org)**

* Fixing `RSSGenerator` test with latest `WeblogPlugin` changes

**2016-12-17  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-20_  Various HADDOCK updates & fixes.

* JSPWiki BLOGS
  Final update of the styling of JSPWiki's BLOGs.
  Also, the fancy weblog calendar is now back in the sidebar
  when viewing a blog post.

* Add-Comment JSP refactored:
  When adding a comment to a wiki-page, you will see the content of the main page
  at the top of the editing screen,  so you know what you are commenting on.
  Improved hover menu on the SAVE/POST button for entering the change-note and
  comment-signature fields.

* Plain Editor:
  Many JS improvements related to the handling of text snippets.
  Several style updates to the editor and the auto-suggest dialogs.

* Small refactoring of the `Install.jsp` to fit the bootstrap framework.

* `%%columns-<width>`: fix the broken width parameter

* `%%graphbars`: fix support for HTML-color-names (chrome, FF)

* [JSPWIKI-979](https://issues.apache.org/jira/browse/JSPWIKI-979): fix support for `%%small` `{{{ preformatted text blocks }}}`

* [JSPWIKI-937](https://issues.apache.org/jira/browse/JSPWIKI-937): fix handling of broken image links (also for FF)
  Fix for rendering of the attachement icon, e.g. in `RecentChanges` page.

* Improved visualisation of interwiki links for Edit, Raw, Reader and Groups.

* The Delete group command now gets you back to the Group view pages,  so it is
  easier for issuing subsequent group commands. (create,edit,delete)

* Added `%%maps` to generate google maps viewer by simply including the address.

* Few html5 FORM improvements: required fields, email input type, ...

* Updated to bootstrap 3.3.7.


**2016-12-13  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-19_

* [JSPWIKI-1032](https://issues.apache.org/jira/browse/JSPWIKI-1032) : Use image `src` attribute instead of `href`

**2016-12-13  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-18_

* [JSPWIKI-1037](https://issues.apache.org/jira/browse/JSPWIKI-1037) UI will not display with IE 9 or IE10.
   Issue with Flexbox implementation in IE.  (also applies to IE11)

* Small style update on `%%categories` dropdown

**2016-12-11  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-17_

* Allow concatenation of css-styles (classes) by using a `.` separator,
  which is useful when adopting styles from the bootstrap framework

  EG. `%%btn.btn-primary.btn-xs This looks like a small Button /%`

**2016-12-03  Harry Metske (metskem AT apache DOT org)**
* Fixed [JSPWIKI-1036](https://issues.apache.org/jira/browse/JSPWIKI-1036) - Search for non-Latin characters fails (reported by Peter Paessler)

**2016-09-16  Harry Metske (metskem AT apache DOT org)**
* Fixed [JSPWIKI-1033](https://issues.apache.org/jira/browse/JSPWIKI-1033) - Incorrect relative navigations (reported by Niklas Polke)

**2016-09-16  David Vittor (dvittor AT apache DOT org)**
* Test commit to our new git repo.

**2016-09-16  Harry Metske (metskem AT apache DOT org)**
* Test commit to see how our new git repo works.

**2016-08-25  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-svn-14_

* Fixed [JSPWIKI-1031](https://issues.apache.org/jira/browse/JSPWIKI-1031) provide stacktrace when throwing `InternalWikiException`, fix by Jürgen Weber.

**2016-08-18  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-svn-13_

* Fixed [JSPWIKI-1029](https://issues.apache.org/jira/browse/JSPWIKI-1029) WebLogic does not find the properties file, fix by Jürgen Weber.

**2016-08-18  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-svn-12_

* Fixed [JSPWIKI-396](https://issues.apache.org/jira/browse/JSPWIKI-396) UTF-8 characters in wiki pages incorrectly rendered if served by Weblogic
  A rigorous fix by Jürgen Weber, ditched `UtilJ2eeCompat`, introduced new property `jspwiki.nofilterencoding`.

**2016-04-17  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-svn-11_

* Fixed [JSPWIKI-936](https://issues.apache.org/jira/browse/JSPWIKI-936) error when remove page with link (Fix by Andrew Krasnoff)

**2016-04-17  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-svn-10_

* Fixed [JSPWIKI-935](https://issues.apache.org/jira/browse/JSPWIKI-935)  `RenderingManager` uses ehcache if `jspwiki.usePageCache = false`

**2016-04-06  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-9_  Few fixes on the HADDOCK template

* improved styling of broken image links

* [JSPWIKI-934](https://issues.apache.org/jira/browse/JSPWIKI-934) Haddock: "page modified" markup differs to the original edits
  Improved styling of the PageModified/Conflict jsp's

* Allow google-fonts in `%%add-css`


**2016-04-03  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-8_  Few more fixes on the HADDOCK template

* Reorganize the attachment detail view, changing the
  order of columns to a more logical format.

* Improve the rendering of the `RecentChanges` page

* Fix the font for text in buttons with an icon

* Fix the popup dialog position in the plain editor
  in case the textarea contains `<`, `>` or `&` characters.

* Hide the section-editlinks for weblog comments.

* Fix the handling of the editor-type switch in the editor.


**2016-03-27  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-7_  Few more small fixes on the HADDOCK template

* [JSPWIKI-918](https://issues.apache.org/jira/browse/JSPWIKI-918) HADDOCK: the "view" menu is back as a better way
  to navigate back to the main page from "Attach" or "Info" views.
  (also removed the ugly "Back to Parent Page" button)

* [JSPWIKI-901](https://issues.apache.org/jira/browse/JSPWIKI-901) : Undo/Redo doesn't work in HADDOCK editor

* [WeblogPlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=WeblogPlugin): added support for filtering weblog entries according to weblog
     start-date and number of days. Now you can select weblog entries
     from a link from the [WeblogArchive](https://jspwiki-wiki.apache.org/Wiki.jsp?page=WeblogArchivePlugin) plugin.

* [JSPWIKI-897](https://issues.apache.org/jira/browse/JSPWIKI-897) : Long page names in Haddock don't wrap gracefully
     Fixing printing issues with long page names.

* Replace the attachment info icon, for not-inlined attachments

* [JSPWIKI-904](https://issues.apache.org/jira/browse/JSPWIKI-904): HADDOCK – the display status of the LeftMenu is retained,
  also after a page refresh.  (by means of a new "Sidebar" user-pref cookie)
  The "Hide-Sidebar" option is obsolete and thus removed from the UserPreferences.


**2016-03-15  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-svn-6_

* Fixed [JSPWIKI-931](https://issues.apache.org/jira/browse/JSPWIKI-931) [VersioningFileProvider](https://jspwiki-wiki.apache.org/Wiki.jsp?page=VersioningFileProvider) sets page author to `unknown` when it should be an authenticated user


**2016-03-12  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-5_

* `%%ADD-CSS`: fix regexp for inline images on IE.

* Fix posting of comments in the Haddock template

* Fixed some missing localization of the weblogplugin.
  Few more tweaks of the styling of weblog entries & comments.


**2016-03-08  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-4_  Few more small fixes related to the HADDOCK template

* Few fixes on well-formed HTML (`SearchBox.jsp`, `PageInfo.jsp`, `Nav.jsp`)

* Fixed some missing localizations in `Nav.jsp`.

* Various improvements of the JSPWiki Weblog implementation and css styling.

* Only show scrollbars on prettified blocks when appropriate (WINDOWS issues)


**2016-03-08  Harry Metske (metskem AT apache DOT org)**

* _2.10.3-svn-3_

* Added `MaxPageNameLength` support in [SpamFilter](https://jspwiki-wiki.apache.org/Wiki.jsp?page=SpamFilter)

**2016-02-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-2_  Few more small fixes on the HADDOCK template

* Added 2 new inter wiki references :
    * `[Raw:MainPage]` for displaying the raw wikimarkup of a page
    * `[Reader:MainPage]` to display a simplified reader view of a page
      (no left menu, layout fit for printing)

* Fixing JS error on <IE11 : `missing Array.from()` (compatibility with mootools 1.5.1)

* Fix for flexbox feature test  (IE)


**2016-02-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.3-svn-1_  Various small fixes on the HADDOCK template

* Fix `%%viewer` "Mixed content" error  (avoid serving http content via https )
* Fix visibility if the Titlebox page has no content
* Add fallback for IE, when the browser doesn't support FLEXBOX support
* Fix scrollbars on prettified sections on Windows (IE, Chrome)
* Add fallback font (SegoeUI) for windows because Helvetica Neue is not supported

* Fix consistency of the styling of the "OK" buttons in many forms.
* Fix indentation of section dropdown entries (plain editor)
* Fix sorting by dates in the Attachment and Info view
* [JSPWIKI-921](https://issues.apache.org/jira/browse/JSPWIKI-921): increase legibility of the plain editor
* [JSPWIKI-928](https://issues.apache.org/jira/browse/JSPWIKI-928): fix odd fonts in the user control panel pop-up window
* Add new Apache `feather.png` as logo background

* Upgrade to mootools 1.6.0


**2016-02-05  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-43_

* Added IP ban support in [SpamFilter](https://jspwiki-wiki.apache.org/Wiki.jsp?page=SpamFilter)

**2016-02-06  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.2-svn-42_

* [JSPWIKI-570](https://issues.apache.org/jira/browse/JSPWIKI-570): Cannot use another `MarkupParser` - hardcoded references to
  `RenderingManager` and `JSPWikiMarkupParser` - thanks to Piotr Tarnowski for all the
  analysis at [JSPWIKI-570](https://issues.apache.org/jira/browse/JSPWIKI-570)

**2016-02-02  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.2-svn-41_

* [JSPWIKI-852](https://issues.apache.org/jira/browse/JSPWIKI-852): `JSPWikiMarkupParser` should report on which page a plugin was missing

**2015-12-09  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.2-svn-40_

* [JSPWIKI-923](https://issues.apache.org/jira/browse/JSPWIKI-923): `doreplace.png` image is missing from [CleanBlue](https://jspwiki-wiki.apache.org/Wiki.jsp?page=CleanBlue) skin

**2015-09-19  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-39_

* [JSPWIKI-916](https://issues.apache.org/jira/browse/JSPWIKI-916) Pre-formatted text within a list renders
  to an unpleasantly small font size, due to relative font sizing.

**2015-09-07  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-38_

* [JSPWIKI-903](https://issues.apache.org/jira/browse/JSPWIKI-903) Fixed a page redirect after attachment delete.

**2015-09-06  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-37_  Few Attachment tweaks in the HADDOCK template

* Fixed display issue with long attachment names
* Show loading animation after pressing the upload button
* Improved display of file sizes with support for KB, MB, GB, TB.


**2015-09-05  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-36_  Improved Attachment upload in the HADDOCK template

* Fixed the `AttachementServlet` so you can now select multiple files
  before pressing the upload button.  You can also use
  drap & drop if your browser supports it.

 * [JSPWIKI-903](https://issues.apache.org/jira/browse/JSPWIKI-903) Fixed a page redirect issue when deleting an
  attachment from the Attachment info page.

* Fixed the zebra-stripes of the FIND `AJAXSearch.jsp`

* Few small improvements on the plain editor suggestion dialogs.


**2015-08-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-35_

* [JSPWIKI-912](https://issues.apache.org/jira/browse/JSPWIKI-912) Haddock Template: fixing the position of headers
  below the fixed navigation bar, when returning from a section edit.


**2015-08-23  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-34_  WYSIWYG improvements

* Added the wysiwyg editor to the wro4j build process

* Improved the stability of the `WysiwygEditingRenderer`


**2015-08-22  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-33_  Fixed [JSPWIKI-910 support configuring jspwiki with envvars
   having dots replaced with underscores in the varnames

**2015-08-16  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-32_  Haddock Template updates & WYSIWYG editing

Haddock Template:

* [JSPWIKI-902](https://issues.apache.org/jira/browse/JSPWIKI-902) Printing improvements for the HADDOCK template,
  hiding sidebar, userbox, wrapping long page-names, and a few other tweaks.

* The Page Footer now sticks to the bottom of the screen, even for short pages

* Fixed an issue with the persistence of the status of collapsible lists

* Added fixes for IE compatibility for the bootstrap css framework.

* [JSPWIKI-892](https://issues.apache.org/jira/browse/JSPWIKI-892) Haddock editor should put the cursor at the top of the textarea,
  also when opening the editor with a certain section

* Improved the header layout (suggestion of Harry) moving the quick search field
  into the searchbox dropdown.

* [JSPWIKI-908](https://issues.apache.org/jira/browse/JSPWIKI-908) The basic editor toolbar icons (bold, italic, link, etc..)
  are back in the plain editor of HADDOCK.


WYSIWYG further enhancements

* (experimental) Included a plain vanilla wysiwyg editor to JSPWiki.
  This editor is based on mooEditable, MIT licensed.
  This editor is unfortunately not compatible with the default template.

  You can still add your own wysiwyg editor to JSPWiki -- hooks are provided
  for TinyMCE and CKeditor.

* Added ajax-based live-preview for wysiwyg editors to the Haddock Template.
  When editing in wysiwyg mode, you now get immediately a preview of the wiki markup.
  (similar to the live-preview mode of the plain editor).

* The wysiwyg editors are now resizable, just like the plain editor.


**2015-08-04  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-31_  Haddock Template small fixes

* Loading error on `haddock.js` and `haddock-edit.js` fixed. (async attribute)
  Was breaking all editing js functions !

* IEx tweaks
    * remove unnecessary scrollbars
    * attempt to resolve the broken icon-fonts on IE11

* `%%add-css` style fix to handle special html entities


**2015-08-04  David Vittor (dvittor AT apache DOT org)**

* _2.10.2-svn-30_

* [JSPWIKI-900](https://issues.apache.org/jira/browse/JSPWIKI-900): Fixed Problem with the `WikiFormsPlugin` Text Area

**2015-08-02  Dirk Frederickx (brushed AT apache DOT org)**

* Fixed the unit tests for `HtmlStringToWikiTranslatorTest`

**2015-08-02  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-29_

Some more Haddock template "tweaks":

* Hide the attach & info navigation menu's for non-existing page

* Improved the Info dropdown when no Author page exists.

* Section titles remain visible, even with a sticky navigation menu.
  Eg. when clicking a table of contents entry, you should end up
  with a visible section header just below the sticky menu line.

* Fixed: the top border of a TABLE was gone.

* Added a version check on the user-preference cookie to be more robust ico changes.
  (hopefully no more cookie clean-up is needed when upgrading JSPWiki)

* Sidebar:
    * The sidebar height now extends till the bottom of the page
    * 3 Sidebar modes are now working: left(default), right and hidden

* Fixed the `<wiki:Link>` tag to support `cssClass` as attribute.

* [JSPWIKI-430](https://issues.apache.org/jira/browse/JSPWIKI-430) All confirmation dialogs are now build with regular DOM elements.
  (check out the Log-out or Delete confirmation dialogs to see the improvement)


WYSIWYG editors:

* Added support for the WYSIWYG editor `TinyMCE.jsp`

* Improved server side handling of `HtmlStringToWiki` translation

* [JSPWIKI-622](https://issues.apache.org/jira/browse/JSPWIKI-622) Added an editor selection switch to the editor toolbar.
  It is now possible to switch between editors (plain, or other installed
  wysiwyg editors) while in Edit. (no need to go first via the Preferences screen)


**2015-07-26  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-28_

Haddock Template commit of the remaining JSP's: UI for handling groups,
workflow UI, and refactored JSP's for Login/Lostpw/Register.
This concludes the re-design of all the haddock template JSP's.

Other changes:

* Fixed page redirections and improved the back button handling.
  Eg. Attachment DELETE will get you now back to the ATTACH view,
  not the INFO view.  See also [JSPWIKI-867](https://issues.apache.org/jira/browse/JSPWIKI-867)

* Tabs & Accordion toggles can now include other markup, rather than only text.

* Added CSS3-based automatic text hyphenation for browsers who support this.
  (also works with none justified text)

* Attachment Upload UI improved: attachment types are represented by icons
  from the icon-font.

* Attachment Info page to access version information on attachments
  should now be more user-friendly, with an extra INFO action button.
  (iso a hidden link via the version number)


**2015-07-16  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-27_

More Haddock Template tweaks:

* Removed annoying scroll-bars from the dropdown menu's which appeared in some browsers

* Improved rendering of inserted dropdown's like the MoreMenu and HomeMenu,
  to make them better fit with bootstrap dropdown menu's.

* Few fixes of the layout of the User Preferences menu, and some refactoring
  of the `UserPreferences.jsp` and the Login.jsp.

* Fixed an editor bug causing the Live Preview to slow down after some time.

WYSIWYG editor in JSPWiki

* Refreshed the WYSIWIG editor with the latest stable version of the
  CKeditor v4.5.1. (replacement of FCK) Standard version, with BootstrapCK4 skin.
  The update was done for both for the default and the Haddock template.
  FFS: server side translation from wiki-markup to HTML needs more work.
  (`WysiwygEditingRenderer.getString()` often crashes )


**2015-07-13  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.2-svn-26_

* [JSPWIKI-899](https://issues.apache.org/jira/browse/JSPWIKI-899): Russian set of wiki pages, contributed by Victor Fedorov, thanks!

**2015-07-12  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-25_

* [JSPWIKI-896](https://issues.apache.org/jira/browse/JSPWIKI-896) Haddock template – user preferences are not saved.
  `haddock-pref.js` was not properly included into the build, due to lowercase
  issue in `wro-haddock.xml`.

* [JSPWIKI-518](https://issues.apache.org/jira/browse/JSPWIKI-518) Saving after editing a section will return you to that section.
  Fixed missing commits on `wiki.js`. Should work now.

* Fixed issue with Accesskey

* [JSPWIKI-433](https://issues.apache.org/jira/browse/JSPWIKI-433) Allow back button for `TAB` keys.
  It is now also possible to click a link to a hidden pane of a tabbed section
  (eg from the Table Of Contents) to automatically open that TAB pane.

* Added a `title` attribute to the pagename in the header.  In case a very long
  pagename is truncated (with ellipsis ...) you can still see the full
  pagename when you hover the mouse over the pagename.


**2015-07-09  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.2-svn-24_

* Minor improvements:
    * Use of `StringBuilder` over `StringBuffer` whenever possible.
    * SLF4J upgraded to 1.7.12
    * JUnit upgraded to 4.12, Jetty upgraded to 8.1.15

**2015-07-05  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-23_

* [JSPWIKI-895](https://issues.apache.org/jira/browse/JSPWIKI-895) Haddock template links contain raw URL information when printed
  including a few tweaks on the print css

* [JSPWIKI-518](https://issues.apache.org/jira/browse/JSPWIKI-518) Saving after editing a section will return you to that  section

* Improved formatting of the Quick Navigation drop-down to show the text and the
  search score on a single line.  (Firefox)


**2015-06-30  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-22_

* [JSPWIKI-894](https://issues.apache.org/jira/browse/JSPWIKI-894) Section editing using Haddock template appears broken.
  Fixed. Also fixed for the default template.


**2015-06-30  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-21_

HADDOCK Template fixes:

* [JSPWIKI-892](https://issues.apache.org/jira/browse/JSPWIKI-892)  Haddock editor when launched is always at bottom of window in Firefox

* Fixing latest update of Icon Styles


**2015-06-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-20_

* [JSPWIKI-891: Fixed annoying jumping behaviour in Firefox of the Haddock editor

* [JSPWIKI-885](https://issues.apache.org/jira/browse/JSPWIKI-885): LivePreview doesn't work
  The HADDOCK template has refresh mechanism based on "change events".
  (no periodic refreshes, like the default template)
  Improved trigger mechanism to catch all keystrokes; and at the same time
  reducing the debounce period (read - refresh time-out) to 1sec.


**2015-06-28  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-19_

More Haddock template fixes

* [JSPWIKI-890](https://issues.apache.org/jira/browse/JSPWIKI-890) (Haddock template) popups dissappear when trying to get
  your mouse to it. Removed the space between the menu and the dropdown.

* [JSPWIKI-887](https://issues.apache.org/jira/browse/JSPWIKI-887) Slimbox image style for embedded images fixed to show
  readable link description even in case of missing *title* or *alt* attributes.

* Updated JSPWikiFont, fixing display issues in FireFox.


**2015-06-27  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-18_

Small fixes and tweaks on haddock template
* Few improvements of the Reader template
* SLIMBOX support for interwiki links
* Small style tweaks
* Fix UserBox issue in non-default language


**2015-06-26  Siegfried Goeschl (sgoeschl@apache.org)**

* Fixed [JSPWIKI-888](https://issues.apache.org/jira/browse/JSPWIKI-888) Enable cache timeouts for Portable JSPWiki.

**2015-06-22  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.2-svn-17_

This is the third major check-in of the HADDOCK template, with mainly
stabilization fixes related for the plain editor, and many UI improvements.
The HADDOCK template is close to completion - go ahead and play with it.
(group related JSPs still to be done)

Summary of main changes:

* Many Suggestion dialogs are added to the plain editor:
  links, link-options, images, acls, plugins, variables, `%%styles`,
  hexadecimal colors, fonts, symbols, `%%icons`, ...
  You can create new suggestion dialogs via json snippets in `Wiki.Snips.js`

* Fixed [JSPWIKI-482](https://issues.apache.org/jira/browse/JSPWIKI-482) Wrong insert from the suggestion box

* The editor toolbar is simplified, as most functions are reachable via the
  suggestion dialogs. The find & replace UI can be repositioned on the screen.

* Sticky menu bar, which stays on top of the screen when scrolling down.

* The Quick Navigation menu is redesigned to improve usability for creating
  and cloning new pages.
  [JSPWIKI-531](https://issues.apache.org/jira/browse/JSPWIKI-531) usability: hints on or mechanism for creating a page

* New `%%styles` added:  `%%dropcaps`, `%%flip`, `%%flop`, `%%addcss`, `%%progress`,
  `%%scrollable-250` (limit the height of a code-block, so it becomes scrollable )

* Show READER view (also great for printing) has been added to the More menu.

* [JSPWIKI-788](https://issues.apache.org/jira/browse/JSPWIKI-788) `TabbedSection` - support multiple tabbedSections in single
  document with same tab-identifiers

* Updated to the latest mootools v1.5.1
* Updated to wro4j 1.7.8

* Some additional i18n properties added -- but translation still to be done.

**2015-05-31  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-16_

* Fixed [JSPWIKI-882](https://issues.apache.org/jira/browse/JSPWIKI-882) test-failure when using existing localized locale in `OutComeTest` (thanks to Marco Roeland)

**2015-04-18  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-15_

* Fixed [JSPWIKI-880](https://issues.apache.org/jira/browse/JSPWIKI-880) Glassfish 4 Wrong Chars Solution - Brasil PT, thanks to Renato Grosz

**2015-03-06  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-14_

* Fixed [JSPWIKI-878](https://issues.apache.org/jira/browse/JSPWIKI-878) (following up [JSPWIKI-660](https://issues.apache.org/jira/browse/JSPWIKI-660)) you can now also use environment variables to configure JSPWiki

**2015-02-12  David Vittor (dvittor AT apache DOT org)**

* _2.10.2-svn-13_

* [Fixed [JSPWIKI-867](https://issues.apache.org/jira/browse/JSPWIKI-867) - Deleting attachments should retain focus on the Attach tab

* [JSPWIKI-566](https://issues.apache.org/jira/browse/JSPWIKI-566) - problem with `Cookie` set preferences and the `GSon.fromJson()` parser

* Fixed search icon in Smart Template `search.gif` instead of `search.png`

**2015-01-30  David Vittor (dvittor AT apache DOT org)**

* _2.10.2-svn-12_

* Fixed [JSPWIKI-566](https://issues.apache.org/jira/browse/JSPWIKI-566) - Complete rewrite of AJAX functionality for JSPWiki

* Fixed [JSPWIKI-502](https://issues.apache.org/jira/browse/JSPWIKI-502) & [JSPWIKI-760](https://issues.apache.org/jira/browse/JSPWIKI-760) - Show Wikipages in Search without Authorization

* Fixed [JSPWIKI-859](https://issues.apache.org/jira/browse/JSPWIKI-859) - Expose the `WikiModuleInfo` to the plugins and filters

* Fixed [JSPWIKI-866](https://issues.apache.org/jira/browse/JSPWIKI-866) - Additional parameters (`url`, `version`, `desc`, `htmltemplate`, `authorurl`) to `jspwiki_module.xml` `WikiModuleInfo`

**2015-01-25  David Vittor (dvittor AT apache DOT org)**

* _2.10.2-svn-11_

* Fixed [JSPWIKI-876](https://issues.apache.org/jira/browse/JSPWIKI-876) - [NotePlugin](https://jspwiki-wiki.apache.org/Wiki.jsp?page=NotePlugin) does not work on wiki without context

* Fixed [JSPWIKI-869](https://issues.apache.org/jira/browse/JSPWIKI-869) - JSPWiki Maven project cannot be imported into Eclipse

* Updated [JSPWIKI-867](https://issues.apache.org/jira/browse/JSPWIKI-867) - Deleting attachments should retain focus on the Attach tab

* Updated [JSPWIKI-566](https://issues.apache.org/jira/browse/JSPWIKI-566) - Some Ajax functionality added - not complete re-write yet

**2014-12-08  Siegfried Goeschl (sgoeschl@apache.org)**

* Fixed [JSPWIKI-829](https://issues.apache.org/jira/browse/JSPWIKI-829) - [Portable] Integrate `jspwiki-portable` into the jspwiki maven build

**2014-11-04  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-10_

* Fixed [JSPWIKI-874](https://issues.apache.org/jira/browse/JSPWIKI-874) - `IllegalStateException` running JSPWiki in Oracle Glassfish Server

**2014-11-04  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-9_

* Fixed [JSPWIKI-871](https://issues.apache.org/jira/browse/JSPWIKI-871) - upgraded nekohtml (0.9.5 => 1.9.21) and xercesImpl (2.4 => 2.10.0)

**2014-11-04  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-8_

* Fixed [JSPWIKI-870](https://issues.apache.org/jira/browse/JSPWIKI-870) - JSPWiki does not start, if tomcat directory path contains a white space.

**2014-09-21  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-7_

* Fixed [JSPWIKI-856](https://issues.apache.org/jira/browse/JSPWIKI-856) - Enhance [FileSystemProvider](https://jspwiki-wiki.apache.org/Wiki.jsp?page=FileSystemProvider) to be able to save page attributes as properties, provided by David Vittor

**2014-08-12  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.2-svn-6_

* Fixed [JSPWIKI-855](https://issues.apache.org/jira/browse/JSPWIKI-855): `NullPointerException` in `FormInput.java:92`, patch provided by Jürgen Weber - thanks!

**2014-07-31  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-5_

* Fixed [JSPWIKI-195](https://issues.apache.org/jira/browse/JSPWIKI-195) - do not allow more than one account with the same email address.
   ==> a new key (`security.error.email.taken`) was added to `CoreResources.properties`

* minor encoding correction for the `CoreResources_nl.properties`

**2014-07-07  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.2-svn-4_

* Dependencies' upgrade: EhCache to 2.6.9, SLF4J to 1.7.7, Selenium to 2.42.0, Stripes to 1.5.7-classloaderfix
  and Jetty to 8.1.15

**2014-06-23  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.2-svn-3_

* Fixed [JSPWIKI-847](https://issues.apache.org/jira/browse/JSPWIKI-847) - Recent Changes Plugin breaks markup if generates an empty table, reported by Dave Koelmeyer

**2014-06-05  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.2-svn-2_

* Fixed [JSPWIKI-843](https://issues.apache.org/jira/browse/JSPWIKI-843) - generate test-jars

* Fixed [JSPWIKI-844](https://issues.apache.org/jira/browse/JSPWIKI-844) - Replace `org.apache.catalina` dependency by applying Ichiro's patch. Thanks!

* Fixed [JSPWIKI-311](https://issues.apache.org/jira/browse/JSPWIKI-311) - Cannot save user profile in container managed authentication mode

* Applied patch on [JSPWIKI-841](https://issues.apache.org/jira/browse/JSPWIKI-841), which solves part of the issue, on unsuccesful login there is no error message
  with container managed authentication

**2014-05-29  Harry Metske (metskem AT apache DOT org)**

* _2.10.2-svn-1_

* Fixed [JSPWIKI-396](https://issues.apache.org/jira/browse/JSPWIKI-396) - by making the server signature comparison case-insensitive (reported by Jürgen Weber)

**2014-05-23  Harry Metske (metskem AT apache DOT org)**

* _2.10.1-svn-17_

* Fixed [JSPWIKI-535](https://issues.apache.org/jira/browse/JSPWIKI-535) - direct links to sections with accents doesn't work

**2014-04-20  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.1-svn-16_

* First steps integrating [Siegfried Goeschl's Wiki On A Stick](https://github.com/sgoeschl/apache-jspwiki)
  (thanks!), portable module still pending.
    * Fixes [JSPWIKI-826](https://issues.apache.org/jira/browse/JSPWIKI-826) - [Portable] PropertyReader ignores the web app class loader

* Lucene updated to 4.7.0

**2014-04-20  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.1-svn-15_

* Fixed [JSPWIKI-822](https://issues.apache.org/jira/browse/JSPWIKI-822) - NPE thrown by `PluginContext#getText()`

* [JSPWIKI-814 - [VersioningFileProvider](https://jspwiki-wiki.apache.org/Wiki.jsp?page=VersioningFileProvider) does migrate original page properties (thanks to Brian Burch)

**2014-04-20  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.1-svn-14_

* Fixed [JSPWIKI-832](https://issues.apache.org/jira/browse/JSPWIKI-832) - [Portable] Problems setting up multiple wikis using a shared JSPWiki libraries
  (patch by Siegfried Goeschl - thanks!)

* Upgraded selenium-*-drivers to 2.41.0

**2014-04-01  Harry Metske (metskem AT apache DOT org)**

* _2.10.1-svn-13_

* Fixed [JSPWIKI-831](https://issues.apache.org/jira/browse/JSPWIKI-831) - Container managed authorization does not work in tomcat

**2014-03-17  Harry Metske (metskem AT apache DOT org)**

* _2.10.1-svn-12_

* Fixed [JSPWIKI-833](https://issues.apache.org/jira/browse/JSPWIKI-833) - temp policy file is created with wrong content (thanks to Dietrich Schmidt)

**2014-03-11  Harry Metske (metskem AT apache DOT org)**

* Fixed [JSPWIKI-823](https://issues.apache.org/jira/browse/JSPWIKI-823) - set `java.io.tmpdir` to `${project.build.directory}` in `pom.xml`

**2014-03-11  Harry Metske (metskem AT apache DOT org)**

* Fixed [JSPWIKI-827](https://issues.apache.org/jira/browse/JSPWIKI-827) - Migrate the `mvn_cheatsheet.txt` to Markdown

**2014-03-03  Harry Metske (metskem AT apache DOT org)**

* _2.10.1-svn-11_

* Fixed [JSPWIKI-813](https://issues.apache.org/jira/browse/JSPWIKI-813) - `ReferenceManagerTest` - two cases fail (thanks to Brian Burch)

**2014-03-02  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.1-svn-10_

This is the second major check-in of the HADDOCK template, with
fixes and improvements mainly related to the plain editor.

* Live preview has been fixed, with ajax based on the fly page rendering.
  The live preview area can now also be displayed side-by-side next to the editor textarea,
  so you can immediately see the rendered wiki-markup during edit.

* Suggestion dialog boxes are shown while you type to assist entrance of more advanced
  wiki-markup such as links, `%%styles`, colors, fonts, plugins, and symbols.
  (but still heavily under development)

* Section Editing has been improved: you can choose which part of the page you want to edit.

* All icons are now based on an icon Font, replacing the FamFamFam icon set.
  Based on Font Awesome by Dave Gandy - http://fontawesome.io | http://fontawesome.io/icons/

* The find & replace UI has been enhanced, showing number of occurrences,
  supporting regular expressions, and supporting replacement for the first or all matches.

* Text is automatically indented based on the indentation level of the previous line.

* Using the `TAB` key inside the textarea will indent a line. Use `shift+TAB` to un-indent.
  Indentation also works when selecting multiple lines.

* You can use `shift+Enter` to quickly insert line-breaks. (\\)


Fixing following editor related JIRA tickets :

* [JSPWIKI-382](https://issues.apache.org/jira/browse/JSPWIKI-382)  Remove `posteditor.js`

* [JSPWIKI-482](https://issues.apache.org/jira/browse/JSPWIKI-482)  Wrong insert from the suggestion box

* [JSPWIKI-443](https://issues.apache.org/jira/browse/JSPWIKI-443)  Full screen editor.
  Added a collapsible sidebar, and a side-by-side display of the live-preview area.

* [JSPWIKI-336](https://issues.apache.org/jira/browse/JSPWIKI-336)  section selection box not working properly. Fixed.

* Fixed the User-Preference page-unload event.


Other changes :

* New "layout" user-preference to switch between fluid or fixed-width page layout.

* Added a info drop-down menu with a summary of the page-info.
  This is similar to page-footer section, but now accessible at the top of the page.

* Replacing all `*.png` based icons by an icon font.  (eg. slimbox, filter, rss-feed )


**2014-02-20  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.1-svn-9_

* JS fix in haddock template : RegExp expression cause FF to crash.


**2014-02-20  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.1-svn-8_

* [JSPWIKI-769 related](https://issues.apache.org/jira/browse/JSPWIKI-769) jspwiki-portable module, right now only Windows executable is generated, cfr. with
  https://jspwiki-wiki.apache.org/Wiki.jsp?page=PortableBinaries

* [JSPWIKI-817 related](https://issues.apache.org/jira/browse/JSPWIKI-817) `Install.jsp` is broken ==> Translation corrections (`install.jsp.intro.[p1|p2|p3]`) for ES

* [JSPWIKI-821](https://issues.apache.org/jira/browse/JSPWIKI-821) `TestEngine` sometimes creates testrepositories with double timestamps after r1567444

**2014-02-18  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.1-svn-7_

* Various small fixes on the HADDOCK template (jsp/css/js) :

* Fixing `%%category` dropdowns which were clipped when inside a `%%accordion`.
  Replacing js based animation by2.10.1-svn-12 css3 animation to show/hide the popup.

* Fixing bug when saving the Preferences (detected by Harry)

* Changed fixed-width layout into fluid layout, occupying all screen real-estate.
  (this could become a user-preference setting in the future)
  Slightly decreasing the size of the sidebar.

**2014-02-18  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.1-svn-6_

* Fixing the `JSONRPCManager.emitJSONCall(..)` so that it now renders
  the correct JSON RPC invocation javascript.

  You can test the `RPCSamplePlugin` like this:
```
[{RPCSamplePlugin

test
}]
```
  We are still getting error-code: `490, "No permission to access this AJAX method!"`
  when invoking a plugin generated json-rpc call.


**2014-02-14  Dirk Frederickx (brushed AT apache DOT org)**

* _2.10.1-svn-5_

  Introducing the HADDOCK template, a new template/ui for Apache JSPWiki.

  This template contains various UI improvements and JSP simplifications,
  a major redesign of the JSPWiki CSS stylesheet based on BOOTSTRAP
  (now modularly build with LESS) and a rework of the javascript routines
  based on mootools v1.4.x. (also the js is now split into modular class files)

  Be aware: this is a first commit -- expect things to be broken.
  More work is needed on the plain editor; the Group UI is to be fixed.
  Validation has been done against Safari, Chrome & FF;  IE testing is left
  to the adventurous user.

  HADDOCK lives peacefully next to the default template. To activate the new
  template, add following line to your `jspwiki-custom.properties`:
```
jspwiki.templateDir = haddock
```

* [JSPWIKI-504](https://issues.apache.org/jira/browse/JSPWIKI-504) New default look for 3.0

* [JSPWIKI-431](https://issues.apache.org/jira/browse/JSPWIKI-431) Attachment Upload, support upload of multiple files, drag&drop,
improved progress bars.
However, the server functionality to upload multiple files is
currently broken. FFS

* [JSPWIKI-432](https://issues.apache.org/jira/browse/JSPWIKI-432) Simplify Tabbed Section Markup
Still maintaining backwards compatibility with the current %%tabbedSection
markup.

* [JSPWIKI-712](https://issues.apache.org/jira/browse/JSPWIKI-712) Entites in ChangeNote should be decoded with "keep editing"

* [JSPWIKI-797](https://issues.apache.org/jira/browse/JSPWIKI-797) Refactoring the JSPWiki javascript routines, upgrade js libs:
    * mootools-core-1.4.5
    * mootools-more-1.4.0.1
    * prettify (dd. 4 mar 2013)

* [JSPWIKI-798](https://issues.apache.org/jira/browse/JSPWIKI-798) Refactoring the JSPWiki main CSS stylesheet -
 now based on the BOOTSTRAP CSS Framework

* [JSPWIKI-430](https://issues.apache.org/jira/browse/JSPWIKI-430) DOM based popups to replace regular js alert or prompt dialog boxes
 Also the edit/clone UI has been refactored.
 Some delete confirmation dialog boxes are still to be converted.

* [JSPWIKI-429](https://issues.apache.org/jira/browse/JSPWIKI-429) Improved SLIMBOX to support Youtube, flash and other formats.
  You can now also 'slimbox' another wiki-page or an external url.
  Based on this media viewer, also a %%carousel style has been added.

* [JSPWIKI-478](https://issues.apache.org/jira/browse/JSPWIKI-478) Remember cookies of collapsible for all contexts
  Collapsible lists and boxes have been refactored, and styled with BOOTSTRAP.
  In general, all %%dynamic-styles are re-styled with BOOTSTRAP css components.

* [JSPWIKI-693](https://issues.apache.org/jira/browse/JSPWIKI-693) style issues

* [JSPWIKI-463](https://issues.apache.org/jira/browse/JSPWIKI-463) display error in default template

* [JSPWIKI-449](https://issues.apache.org/jira/browse/JSPWIKI-449) Menuhide functionality is illogical
  The sidebar (aka Favorites) can be shown/hidden via a toggle button.
  By default, the sidebar is hidden in the Edit/Comment view, to give
  maximum square-meters to the edit text-area.

* [JSPWIKI-512](https://issues.apache.org/jira/browse/JSPWIKI-512) CSS Error with Firefox 2.0.20

*  Upgrade wro4j to latest version, 1.7.3

**2014-02-12  Juan Pablo Santos (juanpablo AT apache DOT org)**

* _2.10.1-svn-4_

* Fixed [JSPWIKI-819](https://issues.apache.org/jira/browse/JSPWIKI-819): Consider replacing ECS with JDOM, thanks to Ichiro Furusato

* `TestEngine( Properties )` uses a different directory as page repo (JSPWIKI-813 related)
