/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wiki.search.kendra;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;
import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.core.Engine;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.search.SearchManager;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.kendra.AWSkendra;
import com.amazonaws.services.kendra.model.BatchPutDocumentRequest;
import com.amazonaws.services.kendra.model.BatchPutDocumentResult;
import com.amazonaws.services.kendra.model.DataSourceSummary;
import com.amazonaws.services.kendra.model.IndexConfigurationSummary;
import com.amazonaws.services.kendra.model.ListDataSourcesRequest;
import com.amazonaws.services.kendra.model.ListDataSourcesResult;
import com.amazonaws.services.kendra.model.ListIndicesRequest;
import com.amazonaws.services.kendra.model.ListIndicesResult;
import com.amazonaws.services.kendra.model.QueryRequest;
import com.amazonaws.services.kendra.model.QueryResult;
import com.amazonaws.services.kendra.model.QueryResultItem;
import com.amazonaws.services.kendra.model.QueryResultType;
import com.amazonaws.services.kendra.model.ScoreAttributes;
import com.amazonaws.services.kendra.model.ScoreConfidence;
import com.amazonaws.services.kendra.model.StartDataSourceSyncJobRequest;
import com.amazonaws.services.kendra.model.StartDataSourceSyncJobResult;
import com.amazonaws.services.kendra.model.TextWithHighlights;

import net.sf.ehcache.CacheManager;
import net.sourceforge.stripes.mock.MockHttpServletRequest;

@ExtendWith(MockitoExtension.class)
public class KendraSearchProviderTest {

  private static final Logger LOG = Logger.getLogger(KendraSearchProviderTest.class);

  TestEngine engine;
  Properties props;
  KendraSearchProvider ksp;

  @Mock
  AWSkendra kendraMock;

  @BeforeEach
  void setUp() throws Exception {
    props = TestEngine.getTestProperties();
    TestEngine.emptyWorkDir(props);
    CacheManager.getInstance().removeAllCaches();
    engine = new TestEngine(props);
    try {
      setupAWSKendra(engine);
    } catch (Exception e) {
      LOG.error(e.toString());
    }
  }

  private void setupAWSKendra(Engine engine) throws InterruptedException {
    ksp = (KendraSearchProvider) engine.getManager(SearchManager.class).getSearchEngine();
    when(kendraMock.listIndices(any(ListIndicesRequest.class))).then(new Answer<ListIndicesResult>() {
      @Override
      public ListIndicesResult answer(InvocationOnMock invocation) throws Throwable {
        return new ListIndicesResult().withIndexConfigurationSummaryItems(
            new IndexConfigurationSummary().withId("IndexId").withName(ksp.getIndexName()));
      }
    });
    lenient().when(kendraMock.listDataSources(any(ListDataSourcesRequest.class))).then(new Answer<ListDataSourcesResult>() {
      @Override
      public ListDataSourcesResult answer(InvocationOnMock invocation) throws Throwable {
        return new ListDataSourcesResult()
            .withSummaryItems(new DataSourceSummary().withId("DataSourceId").withName(ksp.getDataSourceName()));
      }
    });
    lenient().when(kendraMock.startDataSourceSyncJob(any(StartDataSourceSyncJobRequest.class))).then(new Answer<StartDataSourceSyncJobResult>() {
      @Override
      public StartDataSourceSyncJobResult answer(InvocationOnMock invocation) throws Throwable {
        return new StartDataSourceSyncJobResult().withExecutionId("executionId");
      }
    });
    lenient().when(kendraMock.batchPutDocument(any(BatchPutDocumentRequest.class))).then(new Answer<BatchPutDocumentResult>() {
      @Override
      public BatchPutDocumentResult answer(InvocationOnMock invocation) throws Throwable {
        return new BatchPutDocumentResult().withFailedDocuments(new ArrayList<>());
      }
    });
    lenient().when(kendraMock.query(any(QueryRequest.class))).then(new Answer<QueryResult>() {
      @Override
      public QueryResult answer(InvocationOnMock invocation) throws Throwable {
        return new QueryResult().withResultItems(new ArrayList<>());
      }
    });
    
    ksp.setKendra(kendraMock);
    ksp.initializeIndexAndDataSource();
  }

  void debugSearchResults(final Collection<SearchResult> res) {
    res.forEach(next -> {
      System.out.println("page: " + next.getPage());
      for (final String s : next.getContexts()) {
        System.out.println("snippet: " + s);
      }
    });
  }

  Callable<Boolean> findsResultsFor(final Collection<SearchResult> res, final String text) {
    return () -> {
      final MockHttpServletRequest request = engine.newHttpRequest();
      final Context ctx = Wiki.context().create(engine, request, ContextEnum.PAGE_EDIT.getRequestContext());
      final Collection<SearchResult> searchResults = ksp.findPages(text, ctx);
      if (searchResults != null && searchResults.size() > 0) {
        debugSearchResults(searchResults);
        res.addAll(searchResults);
        return true;
      }
      return false;
    };
  }

  @Test
  public void testSimpleSearch() throws Exception {
    final String txt = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.";
    engine.saveText("TestPage", txt);
    addTestresult("TestPage", "mankind", ScoreConfidence.VERY_HIGH);
    final Collection<SearchResult> res = new ArrayList<>();
    Awaitility.await("testSimpleSearch").until(findsResultsFor(res, "mankind"));
    Assertions.assertEquals(1, res.size(), "no pages");
    Assertions.assertEquals("TestPage", res.iterator().next().getPage().getName(), "page");
    engine.deleteTestPage("TestPage");
  }
  
  private void addTestresult(String pageName, String pageContent, ScoreConfidence scoreConfidence ) {
    when(kendraMock.query(any(QueryRequest.class))).then(new Answer<QueryResult>() {
      @Override
      public QueryResult answer(InvocationOnMock invocation) throws Throwable {
        QueryResultItem item = new QueryResultItem().withId(pageName).withType(QueryResultType.DOCUMENT);
        item.withDocumentTitle(new TextWithHighlights().withText(pageName));
        item.withDocumentExcerpt(new TextWithHighlights().withText(pageContent));
        item.withScoreAttributes(new ScoreAttributes().withScoreConfidence(scoreConfidence));
        return new QueryResult().withResultItems(item);
      }
    });
  }
}