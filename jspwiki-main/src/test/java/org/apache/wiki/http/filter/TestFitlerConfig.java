/*
 * Copyright 2026 The Apache Software Foundation.
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
package org.apache.wiki.http.filter;

import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletContext;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import org.apache.wiki.HttpMockFactory;
import org.mockito.Mockito;
import static org.mockito.Mockito.when;

/**
 *
 * =
 */
public class TestFitlerConfig implements FilterConfig {

    public Map<String, String> settings = new HashMap<>();

    @Override
    public String getFilterName() {
        return "Test";
    }

    @Override
    public ServletContext getServletContext() {
        return HttpMockFactory.createServletContext("wiki");
        
    }

    @Override
    public String getInitParameter(String name) {
        return settings.get(name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

}
