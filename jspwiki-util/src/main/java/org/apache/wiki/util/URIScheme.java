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

package org.apache.wiki.util;


import org.apache.commons.lang3.StringUtils;

/**
 * Enumerates supported URI schemes.
 *
 */
public enum URIScheme {

    HTTP("http"), HTTPS("https");

    public final String id;

    URIScheme(final String id) {
        if( StringUtils.isNotBlank( id ) ) {
            this.id = id;
        } else {
            throw new IllegalArgumentException("id must not be blank");
        }
    }

    public String getId() {
        return id;
    }

    public boolean same(final String scheme) {
        return id.equalsIgnoreCase(scheme);
    }

    @Override
    public String toString() {
        return id;
    }

}
