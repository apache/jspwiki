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

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * see JSPWIKI-1289 and JSPWIKI-1294
 *
 * This can be configured either via web.xml using the init parameters
 * (trustedProxies, allowedHosts) or in jspwiki-custom.properties
 * (forwarded.trustedProxies and forwarded.allowedHosts).
 *
 * Used for providing security filtering for when jspwiki is behind a reverse
 * proxy and providing some security checks on inbound http headers.
 *
 * Trust Proxies can either be ip addresses, ip address range via CIDR notation,
 * or hostnames. This would be the ip address/hostnames of your reverse proxy
 * (httpd/nginx/etc)
 *
 * Allowed Hosts is the public facing URL/hostname. i.e. wiki.apache.org or
 * whatever.
 *
 * If both checks do not pass, inbound http headers related to forwarded by are
 * dropped from further processing.
 * 
 * If neither is defined, it will default to a secure default setting, meaning
 * all x forwarded by headers are dropped/ignored
 *
 * @since 3.0.1, 2.12.6
 */
public class ForwardedHeaderFilter implements Filter {

    private static final Logger LOG = LoggerFactory.getLogger(ForwardedHeaderFilter.class);
    /**
     * Peers whose X-Forwarded-* we believe. Empty => trust nobody, strip
     * everything.
     */
    private List<String> trustedProxies = new ArrayList<>();
    /**
     * Acceptable host[:port] values. Empty => only the container's own server
     * name.
     */
    private Set<String> allowedHosts = new HashSet<>();

    @Override
    public void init(final FilterConfig filterConfig) {
        final String proxies = FilterOperations.initValue(filterConfig, "trustedProxies", "forwarded.trustedProxies");
        final String hosts = FilterOperations.initValue(filterConfig, "allowedHosts", "forwarded.allowedHosts");
        // parse comma-separated; proxies as regex on remoteAddr, hosts lowercased literal
        if (proxies != null) {
            for (String s : proxies.split("\\,")) {
                s = s.trim();
                if (s.length() > 0) {
                    trustedProxies.add((s));
                }
            }
        }
        if (hosts != null) {
            for (String s : hosts.split("\\,")) {
                s = s.trim();
                if (s.length() > 0) {
                    allowedHosts.add(s);
                }
            }
        }
    }

    /**
     * Returns whether the given address matches an entry of the
     * {@value #PROP_TRUSTED_PROXIES} system property.
     *
     * @param addr address to check.
     * @return {@code true} if the address is a trusted proxy, {@code false}
     * otherwise.
     */
    boolean isTrustedProxy(final String addr, final String originalHostHeader) {
        LOG.trace("isTrustedProxy " + addr + "," + originalHostHeader);
        if (trustedProxies.isEmpty() || allowedHosts.isEmpty()) {
            LOG.info("web.xml configuration has no trustedProxies or allowedHosts set, meaning we are not behind a reverse proxy. all requests will not honor x-forwarded-by headers");
            return false;
        }
        if (StringUtils.isEmpty(addr)) {
            LOG.info("Request does not contain a Remote Address header that is a trust proxy, denying access to X-Forwarded-By headers");
            return false;
        }
        boolean proxyCheck = false;
        for (final String entry : trustedProxies) {
            if (entry.indexOf('/') >= 0) {
                try {
                    if (new SubnetUtils(entry).getInfo().isInRange(addr)) {
                        LOG.info("Request has Remote Address header that is a trust proxy via CIDR notation, allowing access to X-Forwarded-By headers");
                        proxyCheck = true;
                        break;
                    }
                } catch (final IllegalArgumentException e) {
                    LOG.warn("ignoring invalid {} entry: {}", "trustedProxies", entry);
                }
            } else if (entry.equalsIgnoreCase(addr)) {
                LOG.info("Request has Remote Address header that is a trust proxy via hostname match, allowing access to X-Forwarded-By headers");
                proxyCheck = true;
                break;
            }
        }
        boolean hostCheck = false;
        for (String host : allowedHosts) {
            if (host.equalsIgnoreCase(originalHostHeader)) {
                LOG.info("Request has an allowed X-Forwarded-Host via hostname match, allowing access to X-Forwarded-By headers");
                hostCheck = true;
            }
        }
        if (!hostCheck) {
            LOG.warn("Request does not have an allowed X-Forwarded-Host via hostname match, denying access to X-Forwarded-By headers");
        }
        if (!proxyCheck) {
            LOG.warn("Request does not have a Remote Address header that is a trust proxy, denying access to X-Forwarded-By headers");
        }
        return hostCheck && proxyCheck;
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain)
            throws IOException, ServletException {

        if (request instanceof HttpServletRequest req) {
            String remoteAddr = req.getRemoteAddr();
            String forwardedHost = req.getHeader("X-Forwarded-Host");
            chain.doFilter(new ForwardedRequest(req, isTrustedProxy(
                    remoteAddr,
                    forwardedHost)), response);
        } else {
            LOG.warn("filter request does not extend from HttpServletRequest and thus all forwarded by headers will process unchecked!" + request.getClass().getCanonicalName());
            chain.doFilter(request, response);
        }
    }

}
