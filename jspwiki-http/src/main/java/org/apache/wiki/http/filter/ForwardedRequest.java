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

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import org.apache.wiki.util.URIScheme;

/**
 *
 */
public class ForwardedRequest extends HttpServletRequestWrapper {

    private final boolean isTrusted;
    private final String requestHostname;   // resolved + validated, or null
    private final String requestProtocol;
    private final String clientAddress;
    private final String requestPort;

    ForwardedRequest(final HttpServletRequest req, final boolean trusted) {
        super(req);
        isTrusted = trusted;
        // 1. Only consider X-Forwarded-* when the immediate peer is a trusted proxy.
        // 2. Take the FIRST comma-separated element: mod_proxy APPENDS to a client-supplied
        //    value, yielding "evil.example, wiki.example".
        // 3. Validate the result against allowedHosts; on failure fall back to the container.
        // 4. Validate the plain Host header the same way, regardless of proxy trust.
        if (trusted) {
            clientAddress = req.getHeader("X-Forwarded-For");
            requestHostname = req.getHeader("X-Forwarded-Host");
            requestProtocol = req.getHeader("X-Forwarded-Proto");
            requestPort = req.getHeader("X-Forwarded-Port");
        } else {
            clientAddress = null;
            requestHostname = null;
            requestProtocol = null;
            requestPort = null;
        }
    }

    @Override
    public String getServerName() {
        return requestHostname != null ? stripPort(requestHostname) : super.getServerName();
    }

    @Override
    public int getRemotePort() {
        if (requestPort != null) {
            return Integer.parseInt(requestPort);
        }
        return super.getRemotePort();
    }

    @Override
    public String getRemoteAddr() {
        /*
        Lists the originating client IP address followed by the IP addresses 
        of any intermediate proxies, separated by commas (e.g., clientIP, proxy1IP, proxy2IP).
         */
        if (clientAddress != null) {
            String[] split = clientAddress.split("\\,");
            return split[0];
        }
        return super.getRemoteAddr();
    }

    @Override
    public int getServerPort() {
        if (requestHostname != null) {
            if (requestHostname.contains(":")) {
                return Integer.parseInt(requestHostname.substring(requestHostname.indexOf(":") + 1));
            }
        }

        return super.getServerPort();
    }

    @Override
    public String getScheme() {
        return requestProtocol != null ? requestProtocol : super.getScheme();
    }

    @Override
    public boolean isSecure() {
        return URIScheme.HTTPS.same(getScheme());
    }

    // Hide the raw headers so no future code path can re-read them unvalidated.
    @Override
    public String getHeader(final String name) {
        return isForwardingHeader(name) ? null : super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaders(final String name) {
        return isForwardingHeader(name) ? Collections.emptyEnumeration() : super.getHeaders(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> filtered = new ArrayList<>();
        Enumeration<String> em = super.getHeaderNames();
        while (em.hasMoreElements()) {
            String key = em.nextElement();
            if (!isTrusted && isForwardingHeader(key)) {
                continue;
            }
            filtered.add(key);
        }

        return new Enumeration<String>() {
            int ptr = 0;

            @Override
            public boolean hasMoreElements() {
                if (ptr < filtered.size()) {
                    return true;
                }
                return false;
            }

            @Override
            public String nextElement() {
                String v = filtered.get(ptr);
                ptr++;
                return v;
            }
        };
    }

    private String stripPort(String host) {
        if (host != null && host.contains(":")) {
            return host.substring(0, host.indexOf(":"));
        }
        return host;
    }

    private boolean isForwardingHeader(String name) {
        return name.toLowerCase().startsWith("x-forwarded-");
    }
}
