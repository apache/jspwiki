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
package org.apache.wiki.search.kendra;

import static java.lang.String.format;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.apache.wiki.InternalWikiException;
import org.apache.wiki.WatchDog;
import org.apache.wiki.WikiBackgroundThread;
import org.apache.wiki.api.core.Attachment;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.core.Page;
import org.apache.wiki.api.exceptions.NoRequiredPropertyException;
import org.apache.wiki.api.exceptions.ProviderException;
import org.apache.wiki.api.providers.PageProvider;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.attachment.AttachmentManager;
import org.apache.wiki.auth.AuthorizationManager;
import org.apache.wiki.auth.permissions.PagePermission;
import org.apache.wiki.pages.PageManager;
import org.apache.wiki.search.SearchProvider;
import org.apache.wiki.util.TextUtil;

import com.amazonaws.services.kendra.AWSkendra;
import com.amazonaws.services.kendra.AWSkendraClientBuilder;
import com.amazonaws.services.kendra.model.BatchDeleteDocumentRequest;
import com.amazonaws.services.kendra.model.BatchDeleteDocumentResult;
import com.amazonaws.services.kendra.model.BatchPutDocumentRequest;
import com.amazonaws.services.kendra.model.BatchPutDocumentResponseFailedDocument;
import com.amazonaws.services.kendra.model.BatchPutDocumentResult;
import com.amazonaws.services.kendra.model.ContentType;
import com.amazonaws.services.kendra.model.DataSourceSummary;
import com.amazonaws.services.kendra.model.Document;
import com.amazonaws.services.kendra.model.DocumentAttribute;
import com.amazonaws.services.kendra.model.DocumentAttributeValue;
import com.amazonaws.services.kendra.model.IndexConfigurationSummary;
import com.amazonaws.services.kendra.model.ListDataSourcesRequest;
import com.amazonaws.services.kendra.model.ListDataSourcesResult;
import com.amazonaws.services.kendra.model.ListIndicesRequest;
import com.amazonaws.services.kendra.model.ListIndicesResult;
import com.amazonaws.services.kendra.model.QueryRequest;
import com.amazonaws.services.kendra.model.QueryResultItem;
import com.amazonaws.services.kendra.model.QueryResultType;
import com.amazonaws.services.kendra.model.ScoreConfidence;
import com.amazonaws.services.kendra.model.StartDataSourceSyncJobRequest;
import com.amazonaws.services.kendra.model.StartDataSourceSyncJobResult;
import com.amazonaws.services.kendra.model.StopDataSourceSyncJobRequest;
import com.amazonaws.services.kendra.model.ThrottlingException;
import com.amazonaws.util.IOUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

/**
 * Search provider that implements {link SearchProvider} using AWS Kendra for
 * indexing. Note that we are using a Custom DataSource which limits the
 * attributes that can be uploaded / searched for each page (as per
 * https://docs.aws.amazon.com/kendra/latest/dg/custom-attributes.html). This
 * could be overcome by using an S3 bucket where any custom attributes can be
 * added.
 *
 * @since 2.11.0
 */
public class KendraSearchProvider implements SearchProvider {

  private static final Logger LOG = Logger.getLogger(KendraSearchProvider.class);
  private Engine engine;
  private Properties properties;
  private Map<String, Object> contentTypes;
  private AWSkendra kendra;
  private String indexName;
  private String indexId;
  private String dataSourceName;
  private String dataSourceId;

  private List<Page> updates = Collections.synchronizedList(new ArrayList<>());

  private static final String PROP_KENDRA_INDEX_NAME = "jspwiki.kendra.indexName";
  private static final String PROP_KENDRA_DATA_SOURCE_NAME = "jspwiki.kendra.dataSourceName";
  private static final String PROP_KENDRA_INDEXDELAY = "jspwiki.kendra.indexdelay";
  private static final String PROP_KENDRA_INITIALDELAY = "jspwiki.kendra.initialdelay";

  public KendraSearchProvider() {
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void initialize(Engine engine, Properties properties) throws NoRequiredPropertyException, IOException {
    this.engine = engine;
    this.properties = properties;
    this.contentTypes = getContentTypes();

    setKendra(buildClient());

    this.indexName = TextUtil.getRequiredProperty(this.properties, PROP_KENDRA_INDEX_NAME);
    this.dataSourceName = TextUtil.getRequiredProperty(this.properties, PROP_KENDRA_DATA_SOURCE_NAME);
    int initialDelay = TextUtil.getIntegerProperty(this.properties, PROP_KENDRA_INITIALDELAY,
        KendraUpdater.INITIAL_DELAY);
    int indexDelay = TextUtil.getIntegerProperty(this.properties, PROP_KENDRA_INDEXDELAY, KendraUpdater.INDEX_DELAY);

    // Start the Kendra update thread, which waits first for a little while
    // before starting to go through the "pages that need updating".
    KendraUpdater updater = new KendraUpdater(engine, this, initialDelay, indexDelay);
    updater.start();
  }

  private Map<String, Object> getContentTypes() {
    Gson gson = new GsonBuilder().create();
    try (InputStream in = KendraSearchProvider.class.getResourceAsStream("content_types.json")) {
      if (in != null) {
        Type collectionType = new TypeToken<HashMap<String, Object>>(){}.getType();
        return gson.fromJson(new InputStreamReader(in), collectionType);
      }
    } catch (IOException e) {
      LOG.error(format("Unable to load default propertyfile 'content_types.json': %s", e.getMessage()), e);
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getProviderInfo() {
    return "KendraSearchProvider";
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void pageRemoved(Page page) {
    String pageName = page.getName();
    BatchDeleteDocumentRequest request = new BatchDeleteDocumentRequest().withIndexId(indexId)
        .withDocumentIdList(pageName);
    BatchDeleteDocumentResult result = getKendra().batchDeleteDocument(request);
    if (result.getFailedDocuments().size() == 0) {
      LOG.debug(format("Page '%s' was removed from index", pageName));
    } else {
      LOG.error(format("Failed to remove Page '%s' from index", pageName));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void reindexPage(Page page) {
    if (page != null) {
      updates.add(page);
      LOG.debug(format("Scheduling page '%s' for indexing ...", page.getName()));
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<SearchResult> findPages(String query, Context wikiContext) throws ProviderException, IOException {
    QueryRequest request = new QueryRequest().withIndexId(indexId).withQueryText(query);
    List<QueryResultItem> items = null;
    try {
      items = getKendra().query(request).getResultItems();
    } catch (ThrottlingException e) {
      LOG.error(format("ThrottlingException. Skipping..."));
      return new ArrayList<>();
    }
    List<SearchResult> searchResults = new ArrayList<>(items.size());
    AuthorizationManager mgr = engine.getManager(AuthorizationManager.class);

    for (QueryResultItem item : items) {
      switch (QueryResultType.fromValue(item.getType())) {
      case DOCUMENT:
        String documentId = item.getDocumentId();
        String documentExcerpt = item.getDocumentExcerpt().getText();
        String scoreConfidence = item.getScoreAttributes().getScoreConfidence();
        Page page = this.engine.getManager(PageManager.class).getPage(documentId, PageProvider.LATEST_VERSION);
        if (page != null) {
          PagePermission pp = new PagePermission(page, PagePermission.VIEW_ACTION);
          if (mgr.checkPermission(wikiContext.getWikiSession(), pp)) {
            SearchResult searchResult = new SearchResultImpl(page, confidence2score(scoreConfidence),
                new String[] { documentExcerpt });
            searchResults.add(searchResult);
          } else {
            LOG.error(format("Page '%s' is not accessible", documentId));
          }
        } else {
          LOG.error(
              format("Kendra found a result page '%s' that could not be loaded, removing from index", documentId));
          pageRemoved(Wiki.contents().page(this.engine, documentId));
        }
        break;
      default:
        LOG.error(format("Unknown query result type: %s", item.getType()));
      }
    }
    return searchResults;
  }

  /**
   * This method initialize the AWS Kendra Index and Datasources to be used.
   * 
   * @throws InterruptedException
   */
  public void initializeIndexAndDataSource() throws InterruptedException {
    this.indexId = getIndexId(indexName);
    if (this.indexId == null) {
      String message = format("Index '%s' does not exists", indexName);
      LOG.error(message);
      throw new IllegalArgumentException(message);
    }
    this.dataSourceId = getDatasourceId(this.indexId, dataSourceName);
    if (this.dataSourceId == null) {
      String message = format("Datasource '%s' does not exists in index %s", dataSourceName, indexName);
      LOG.error(message);
      throw new IllegalArgumentException(message);
    }
  }

  /**
   * Given an Kendra's Index name, returns the corresponding Index Id, or
   * {@code null} if it does not exists
   * 
   * @param indexName the name of the index to look up
   * @return the index id or {@code null}
   */
  private String getIndexId(String indexName) {
    ListIndicesRequest request = new ListIndicesRequest();
    ListIndicesResult result = getKendra().listIndices(request);
    String nextToken = "";
    while (nextToken != null) {
      for (IndexConfigurationSummary item : result.getIndexConfigurationSummaryItems()) {
        if (StringUtils.equals(item.getName(), indexName)) {
          return item.getId();
        }
      }
      nextToken = result.getNextToken();
      request = new ListIndicesRequest().withNextToken(result.getNextToken());
      result = getKendra().listIndices(request);
    }
    return null;
  }

  /**
   * Given an Kendra's Datasource name, returns the corresponding Datasource Id,
   * or {@code null} if it does not exists
   * 
   * @param dataSourceName the name of the datasource to look up
   * @return the datasource id or {@code null}
   */
  private String getDatasourceId(String indexId, String dataSourceName) {
    ListDataSourcesRequest request = new ListDataSourcesRequest().withIndexId(indexId);
    ListDataSourcesResult result = getKendra().listDataSources(request);
    String nextToken = "";
    while (nextToken != null) {
      for (DataSourceSummary item : result.getSummaryItems()) {
        if (StringUtils.equals(item.getName(), dataSourceName)) {
          return item.getId();
        }
      }
      nextToken = result.getNextToken();
      request = new ListDataSourcesRequest().withNextToken(result.getNextToken());
      result = getKendra().listDataSources(request);
    }
    return null;
  }

  /*
   * Converts a SCORE Confidence from Kendra to an "equivalent" integer score
   */
  private int confidence2score(String scoreConfidence) {
    switch (ScoreConfidence.fromValue(scoreConfidence)) {
    case VERY_HIGH:
      return 100;
    case HIGH:
      return 75;
    case MEDIUM:
      return 50;
    case LOW:
      return 25;
    default:
      return 0;
    }
  }
  
  /**
   * This method re-index all the pages found in the Wiki. It is mainly used at
   * startup.
   * 
   * @throws IOException in case some page can not be read
   */
  private void doFullReindex() throws IOException {
    try {
      Collection<Page> pages = engine.getManager(PageManager.class).getAllPages();
      if (pages.isEmpty()) {
        return;
      }
      LOG.debug(format("Indexing all %d pages. Please wait ...", pages.size()));
      String executionId = startExecution();
      for (Page page : pages) {
        // Since I do not want to handle the size limit
        // (https://docs.aws.amazon.com/goto/WebAPI/kendra-2019-02-03/BatchPutDocument)
        // uploading documents one at a time
        indexOnePage(page, executionId);
      }
    } catch (ProviderException e) {
      LOG.error(e.getMessage());
      throw new IOException(e);
    } finally {
      stopExecution();
    }
  }

  /**
   * This method re-index all pages marked as updated. It is used to periodically
   * index pages that have been modified
   */
  private void doPartialReindex() {
    if (updates.isEmpty()) {
      return;
    }
    LOG.debug(format("Indexing updated pages. Please wait ..."));
    String executionId = startExecution();
    synchronized (updates) {
      try {
        while (updates.size() > 0) {
          indexOnePage(updates.remove(0), executionId);
        }
      } finally {
        stopExecution();
      }
    }
  }

  /**
   * Returns an ExecutiuonId that is required to keep track of the modifed 
   * documents
   * @return The execution id
   */
  private String startExecution() {
    StartDataSourceSyncJobRequest request = new StartDataSourceSyncJobRequest().withIndexId(indexId)
        .withId(dataSourceId);
    StartDataSourceSyncJobResult result = getKendra().startDataSourceSyncJob(request);
    return result.getExecutionId();
  }

  /**
   * Stop the execution for the given index Id and DataSource Id.
   */
  private void stopExecution() {
    StopDataSourceSyncJobRequest request = new StopDataSourceSyncJobRequest().withIndexId(indexId).withId(dataSourceId);
    getKendra().stopDataSourceSyncJob(request);
  }
  
  /**
   * Index on single {@link Page} into the Kendra Index
   * @param page the {@link Page} to index
   * @param executionId The Execution Id
   */
  private void indexOnePage(Page page, String executionId) {
    String pageName = page.getName();
    try {
      Document document = newDocument(page, executionId);
      BatchPutDocumentRequest request = new BatchPutDocumentRequest().withIndexId(indexId)
          .withDocuments(document);
      BatchPutDocumentResult result = getKendra().batchPutDocument(request);
      if (result.getFailedDocuments().size() == 0) {
        LOG.info(format("Successfully indexed Page '%s' as %s", page.getName(), document.getContentType()));
      } else {
        for (BatchPutDocumentResponseFailedDocument failedDocument : result.getFailedDocuments()) {
          LOG.error(format("Failed to index Page '%s': %s", failedDocument.getId(), failedDocument.getErrorMessage()));
        }
      }
    } catch (IOException e) {
      LOG.error(format("Failed to index Page '%s': %s", pageName, e.getMessage()));
    }
  }


  /**
   * Given a {@link Page}, returns the corresponding Kendra {@link Document}.
   * 
   * @param page        the {@link Page} to be indexed
   * @param executionId an execution id to identify when the {@link Page} was
   *                    indexed for the last time.
   * @return a {@link Document} containing the searchable attributes.
   * @throws IOException if the {@link Page}'s {@link Attachment} can not be read.
   */
  private Document newDocument(Page page, String executionId) throws IOException {
    String pageName = page.getName();
    List<DocumentAttribute> attrs = new ArrayList<>();
    // These 2 are required as per
    // https://docs.aws.amazon.com/kendra/latest/dg/data-source-custom.html#custom-required-attributes
    attrs.add(newAttribute("_data_source_id", dataSourceId));
    attrs.add(newAttribute("_data_source_sync_job_execution_id", executionId));

    String title = TextUtil.beautifyString(pageName);
    ByteBuffer blob = null;
    ContentType contentType = ContentType.PLAIN_TEXT;
    if (page instanceof Attachment) {
      Attachment attachment = (Attachment) page;
      InputStream is = null;
      try {
        String filename = attachment.getFileName();
        contentType = getContentType(filename);
        is = engine.getManager(AttachmentManager.class).getAttachmentStream(attachment);
        blob = ByteBuffer.wrap(IOUtils.toByteArray(is));
      } catch (ProviderException e) {
        throw new IOException(e);
      } finally {
        IOUtils.closeQuietly(is, null);
      }
      // contentType should be set to its real value
    } else {
      String text = engine.getManager(PageManager.class).getPureText(page);
      blob = ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8));
    }
    return new Document().withId(pageName).withTitle(title).withAttributes(attrs).withBlob(blob)
        .withContentType(contentType);
  }

  private DocumentAttribute newAttribute(String key, String value) {
    return new DocumentAttribute().withKey(key).withValue(new DocumentAttributeValue().withStringValue(value));
  }

  @SuppressWarnings("unchecked")
  private ContentType getContentType(String filename) {
    String extention = FilenameUtils.getExtension(filename);
    Map<String, String> ct = (Map<String, String>) this.contentTypes.get("ContentTypes");
    return ContentType.fromValue(ct.getOrDefault(extention, ContentType.PLAIN_TEXT.name()));
  }

  /**
   * Updater thread that updates Kendra indexes.
   */
  private static final class KendraUpdater extends WikiBackgroundThread {
    protected static final int INDEX_DELAY = 5;
    protected static final int INITIAL_DELAY = 10;
    private KendraSearchProvider provider;

    private int initialDelay;

    private WatchDog watchdog;

    private KendraUpdater(Engine engine, KendraSearchProvider provider, int initialDelay, int indexDelay) {
      super(engine, indexDelay);
      this.provider = provider;
      this.initialDelay = initialDelay;
      setName("JSPWiki Kendra Indexer");
    }

    @Override
    public void startupTask() throws Exception {
      watchdog = WatchDog.getCurrentWatchDog(getEngine());
      try {
        Thread.sleep(initialDelay * 1000L);
      } catch (InterruptedException e) {
        throw new InternalWikiException("Interrupted while waiting to start.", e);
      }
      watchdog.enterState("Full reindex");
      provider.initializeIndexAndDataSource();
      provider.doFullReindex();
      watchdog.exitState();
    }

    @Override
    public void backgroundTask() {
      watchdog.enterState("Reindexing ...", 60);
      provider.doPartialReindex();
      watchdog.exitState();
    }
  }

  private static class SearchResultImpl implements SearchResult {

    private Page page;
    private int score;
    private String[] contexts;

    public SearchResultImpl(Page page, int score, String[] contexts) {
      this.page = page;
      this.score = score;
      this.contexts = contexts != null ? contexts.clone() : null;
    }

    @Override
    public Page getPage() {
      return this.page;
    }

    @Override
    public int getScore() {
      return this.score;
    }

    @Override
    public String[] getContexts() {
      return this.contexts;
    }
  }

  public AWSkendra getKendra() {
    return kendra;
  }

  public void setKendra(AWSkendra kendra) {
    this.kendra = kendra;
  }

  protected AWSkendra buildClient() {
    return AWSkendraClientBuilder.defaultClient();
  }

  public String getIndexName() {
    return indexName;
  }

  public String getDataSourceName() {
    return dataSourceName;
  }

}