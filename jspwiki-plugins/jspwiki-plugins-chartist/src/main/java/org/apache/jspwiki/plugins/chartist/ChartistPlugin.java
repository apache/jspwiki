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

package org.apache.jspwiki.plugins.chartist;

import java.util.Locale;
import java.util.Map;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.exceptions.PluginException;
import org.apache.wiki.api.plugin.Plugin;

/**
 * Chartlist-js plugin for JSPWiki.
 * 
 * See readme for deployment and usage guide.
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
                  script.src = 'chartist-plugin/index.umd.js';
                  document.head.appendChild(script);
                  
                  const behavior = document.createElement('script');
                behavior.src = 'chartist-plugin/Wiki.Chartist.Behavior.js';
                document.head.appendChild(behavior);
                  
                  const stylesheet = document.createElement('link');
                  stylesheet.rel = 'stylesheet';
                  stylesheet.type = 'text/css';
                  stylesheet.href = 'chartist-plugin/index.css'; 
                  document.head.appendChild(stylesheet);
                  </script>
                                    
                  """);
        //<link rel="stylesheet" href="webjars/chartist/dist/index.css" />
        //<script type="text/javascript" src="webjars/chartist/dist/index.umd.js"></script>

        return sb.toString();
    }

    @Override
    public String getSnipExample() {
        return Plugin.super.getSnipExample();
    }

    @Override
    public String getDisplayName(Locale locale) {
        return Plugin.super.getDisplayName(locale);
    }
}

