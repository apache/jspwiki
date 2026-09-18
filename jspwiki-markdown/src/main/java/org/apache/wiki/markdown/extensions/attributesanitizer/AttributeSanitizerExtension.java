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
package org.apache.wiki.markdown.extensions.attributesanitizer;

import com.vladsch.flexmark.html.AttributeProvider;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.html.IndependentAttributeProviderFactory;
import com.vladsch.flexmark.html.renderer.AttributablePart;
import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.html.MutableAttributes;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Set;

/**
 * {@link HtmlRenderer.HtmlRendererExtension} which strips dangerous
 * author-supplied attributes from the rendered HTML. Flexmark's
 * AttributesExtension applies attribute lists such as
 * <code>{onclick=...}</code> written by page authors to the rendered elements
 * without any filtering, so without this extension any author could attach
 * <code>on*</code> event handlers (stored XSS), <code>style</code> overlays or
 * <code>javascript:</code> URLs to rendered content.
 * {@code HtmlRenderer.ESCAPE_HTML} does not cover extension-assigned
 * attributes.
 *
 * <p>
 * This extension must be registered <em>after</em> {@code AttributesExtension},
 * so its attribute provider runs last and sees the final attribute set of each
 * node.</p>
 */
public class AttributeSanitizerExtension implements HtmlRenderer.HtmlRendererExtension {

    /**
     * Attributes whose value is a URL, and must therefore not carry a
     * script-capable scheme.
     */
    private static final Set< String> URL_ATTRIBUTES = Set.of(""
            + "href", "src", "xlink:href",
            "action", "formaction",
            "background", "background-color", 
            "poster", "data", "cite");

    public static AttributeSanitizerExtension create() {
        return new AttributeSanitizerExtension();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void rendererOptions(final MutableDataHolder options) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void extend(final HtmlRenderer.Builder rendererBuilder, final String rendererType) {
        rendererBuilder.attributeProviderFactory(new IndependentAttributeProviderFactory() {

            @Override
            public AttributeProvider apply(final LinkResolverContext context) {
                return AttributeSanitizerExtension::sanitize;
            }

        });
    }

    /**
     * Removes every <code>on*</code> event handler, the <code>style</code> and
     * <code>srcdoc</code> attributes, and any URL-valued attribute whose value
     * carries a script-capable scheme.
     *
     * @param node the node being rendered
     * @param part the attributable part of the node
     * @param attributes the final attribute set of the node, mutated in place
     */
    static void sanitize(final Node node, final AttributablePart part, final MutableAttributes attributes) {
        for (final String name : new ArrayList<>(attributes.keySet())) {
            final String attribute = name.trim().toLowerCase(Locale.ENGLISH);
            if (attribute.startsWith("on") || "style".equals(attribute) || "srcdoc".equals(attribute)) {
                attributes.remove(name);
            } else if (URL_ATTRIBUTES.contains(attribute) && hasForbiddenScheme(attributes.getValue(name))) {
                attributes.remove(name);
            }
        }
    }

    static boolean hasForbiddenScheme(final String value) {
        if (value == null) {
            return false;
        }
        // browsers ignore ASCII control characters and whitespace when parsing URL schemes, so strip them first
        final String url = value.toLowerCase(Locale.ENGLISH).replaceAll("[\\x00-\\x20]", "");
        return url.startsWith("javascript:") || url.startsWith("vbscript:") || url.startsWith("data:");
    }

}
