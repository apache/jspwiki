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
package org.apache.wiki.search.tika;

import org.apache.log4j.Logger;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.ClimateForcast;
import org.apache.tika.metadata.CreativeCommons;
import org.apache.tika.metadata.Database;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.IPTC;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.Office;
import org.apache.tika.metadata.OfficeOpenXMLCore;
import org.apache.tika.metadata.PDF;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.attachment.Attachment;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.search.LuceneSearchProvider;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

/**
 * Search provider that extends {link LuceneSearchProvider} using Apache Tika for indexing attachment content.
 *
 * @since 2.11.0
 * @see <a href="https://issues.apache.org/jira/browse/JSPWIKI-469">JSPWIKI-469</a>
 */
public class TikaSearchProvider extends LuceneSearchProvider {

    private static final Logger LOG = Logger.getLogger( TikaSearchProvider.class );
    AutoDetectParser parser;
    Set< String > textualMetadataFields;

    public TikaSearchProvider() {
        parser = new AutoDetectParser();

        // metadata fields that also are indexed
        textualMetadataFields = new HashSet<>();
        textualMetadataFields.add( TikaCoreProperties.TITLE.getName() );
        textualMetadataFields.add( TikaCoreProperties.COMMENTS.getName() );
        textualMetadataFields.add( TikaCoreProperties.KEYWORDS.getName() );
        textualMetadataFields.add( TikaCoreProperties.DESCRIPTION.getName() );
        textualMetadataFields.add( TikaCoreProperties.TYPE.getName() );
        textualMetadataFields.add( TikaMetadataKeys.RESOURCE_NAME_KEY );
        textualMetadataFields.add( PDF.DOC_INFO_TITLE.getName() );
        textualMetadataFields.add( PDF.DOC_INFO_KEY_WORDS.getName() );
        textualMetadataFields.add( PDF.DOC_INFO_SUBJECT.getName() );
        textualMetadataFields.add( OfficeOpenXMLCore.SUBJECT.getName() );
        textualMetadataFields.add( Office.KEYWORDS.getName() );
        textualMetadataFields.add( TikaCoreProperties.TYPE.getName() );
        textualMetadataFields.add( HttpHeaders.CONTENT_TYPE );
        textualMetadataFields.add( IPTC.HEADLINE.getName() );
        textualMetadataFields.add( Database.COLUMN_NAME.getName() );
        textualMetadataFields.add( Database.TABLE_NAME.getName() );
        textualMetadataFields.add( CreativeCommons.WORK_TYPE );
        textualMetadataFields.add( ClimateForcast.COMMENT );
        textualMetadataFields.add( ClimateForcast.HISTORY );
        textualMetadataFields.add( ClimateForcast.INSTITUTION );
    }

    /**
     * {@inheritDoc}
     * @param att Attachment to get content for. Filename extension is used to determine the type of the attachment.
     * @return String representing the content of the file.
     */
    @Override
    protected String getAttachmentContent( final Attachment att ) {
        // LOG.debug("indexing "+att.getFileName());
        final AttachmentManager mgr = getEngine().getAttachmentManager();
        final StringBuilder out = new StringBuilder();

        try( final InputStream attStream = mgr.getAttachmentStream( att ) ) {
            final Metadata metadata = new Metadata();
            metadata.set( TikaMetadataKeys.RESOURCE_NAME_KEY, att.getFileName() );

            final ContentHandler handler = new BodyContentHandler(-1 );
            // -1 disables the character size limit; otherwise only the first 100.000 characters are indexed

            parser.parse( attStream, handler, metadata );
            out.append( handler.toString() );

            final String[] names = metadata.names();
            for( int j = 0; j < names.length; j++ ) {
                if( textualMetadataFields.contains( names[ j ] ) ) {
                    out.append( " " ).append( metadata.get( names[ j ] ) );
                }
            }
        } catch( TikaException | SAXException e ) {
            LOG.error( "Attachment cannot be parsed", e );
        } catch( ProviderException | IOException e ) {
            LOG.error( "Attachment cannot be loaded", e );
        }

        return out.toString();
    }

}