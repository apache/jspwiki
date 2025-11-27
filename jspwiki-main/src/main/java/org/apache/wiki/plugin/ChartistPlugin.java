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
package org.apache.wiki.plugin;

import java.util.Locale;
import java.util.Map;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;

/**
 * Charts and graphis via chartist-js this plugin enables a specific tag on the
 * page that enables charting capabilities on the wiki page. It should be here
 * the top.
 *
 * Because there was some concerns from some team members about including
 * chartist on all pages by default, this plugin injects a script into the page
 * to dynamically add the css and javascript to the page that effectively adds
 * the chartist libraries (css and js) to the current page's context. This then
 * enables the behavior to fire off and convert tables to a cool chart.
 *
 *
 *
 * @since 3.0.0
 */
public class ChartistPlugin implements Plugin {

    @Override
    public String execute(Context context, Map<String, String> params) throws PluginException {
        StringBuilder sb = new StringBuilder();
        sb.append("""
                  <style type="text/css">
                  .chartist-options,.chartist-table{
                      display:none;
                  }
                  .ct-chart .ct-label,.ct-chart .ct-label.ct-vertical,.ct-chart .ct-label.ct-horizontal{
                      font-size:1em;
                  }
                  .ct-chart-pie .ct-label{
                      fill:rgba(255,255,255,.8);
                  }
                  
                  </style>
                  <script>
                  const script = document.createElement('script');
                  script.src = 'webjars/chartist/dist/index.umd.js';
                  document.head.appendChild(script);
                  
                  const stylesheet = document.createElement('link');
                  stylesheet.rel = 'stylesheet';
                  stylesheet.type = 'text/css';
                  stylesheet.href = 'webjars/chartist/dist/index.css'; 
                  document.head.appendChild(stylesheet);
                  </script>
                                    
                  """);
        //<link rel="stylesheet" href="webjars/chartist/dist/index.css" />
        //<script type="text/javascript" src="webjars/chartist/dist/index.umd.js"></script>

        return sb.toString();
    }

    @Override
    public String getSnipExample() {
        return Plugin.super.getSnipExample(); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

    @Override
    public String getDisplayName(Locale locale) {
        return Plugin.super.getDisplayName(locale); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/OverriddenMethodBody
    }

}
