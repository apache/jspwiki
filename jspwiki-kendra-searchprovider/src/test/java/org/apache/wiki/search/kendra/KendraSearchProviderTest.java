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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.apache.wiki.TestEngine;
import org.apache.wiki.api.core.Context;
import org.apache.wiki.api.core.ContextEnum;
import org.apache.wiki.api.exceptions.WikiException;
import org.apache.wiki.api.search.SearchResult;
import org.apache.wiki.api.spi.Wiki;
import org.apache.wiki.search.SearchManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.platform.commons.util.StringUtils;
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

  TestEngine engine;
  Properties props;
  KendraSearchProvider searchProvider;

  @Mock
  AWSkendra kendraMock;

  @BeforeEach
  void setUp(TestInfo testInfo) throws Exception {
    props = TestEngine.getTestProperties();
    TestEngine.emptyWorkDir(props);
    CacheManager.getInstance().removeAllCaches();
    engine = new TestEngine(props);

    // before each test I setup the Kendra Client
    searchProvider = (KendraSearchProvider) engine.getManager(SearchManager.class).getSearchEngine();
    Method m = testInfo.getTestMethod().get();
    String indexName = null;
    String dataSourceName = null;
    if (m.isAnnotationPresent(WithKendra.class)) {
      WithKendra withKendra = m.getAnnotation(WithKendra.class);
      indexName = withKendra.indexName();
      dataSourceName = withKendra.dataSourceName();
      setUpKendraMock(indexName, dataSourceName);
      searchProvider.setKendra(kendraMock);
      if (StringUtils.isNotBlank(indexName) && StringUtils.isNotBlank(dataSourceName)) {
        searchProvider.initializeIndexAndDataSource();
      }
    } else {
      setUpKendraMock(null, null);
      searchProvider.setKendra(kendraMock);
    }
    // And possibly the pages that will be present in the wiki
    if (m.isAnnotationPresent(WithPages.class)) {
      WithPages withPages = m.getAnnotation(WithPages.class);
      addPages(withPages.value());
    }
    if (m.isAnnotationPresent(WithPage.class)) {
      WithPage withPage = m.getAnnotation(WithPage.class);
      addPages(withPage);
    }
    // and the corresponding search results
    if (m.isAnnotationPresent(WithResults.class)) {
      WithResults withResults = m.getAnnotation(WithResults.class);
      addResults(withResults.value());
    }
    if(m.isAnnotationPresent(WithResult.class)) {
      WithResult withResult = m.getAnnotation(WithResult.class);
      addResults(withResult);
    }
  }

  @AfterEach
  void tearDown(TestInfo testInfo) throws Exception {
    Method m = testInfo.getTestMethod().get();
    // And possibly the pages that will be present in the wiki
    if (m.isAnnotationPresent(WithPage.class)) {
      WithPage withPage = m.getAnnotation(WithPage.class);
      engine.deleteTestPage(withPage.name());
    }
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
      final Collection<SearchResult> searchResults = searchProvider.findPages(text, ctx);
      if (searchResults != null && !searchResults.isEmpty()) {
        debugSearchResults(searchResults);
        res.addAll(searchResults);
        return true;
      }
      return false;
    };
  }

  @Test
  public void testInvalidIndexName() throws Exception {
    // IndexName is invalid...
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      searchProvider.initializeIndexAndDataSource();
    });
  }

  @Test
  @WithKendra(indexName = "JSPWikiIndex", dataSourceName = "")
  public void testInvalidDataSourceName() throws Exception {
    // IndexName is invalid...
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      searchProvider.initializeIndexAndDataSource();
    });
  }

  @Test
  @WithKendra(indexName = "JSPWikiIndex", dataSourceName = "JSPWikiDataSource")
  @WithPage(name = "TestPage", text = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.", attachments = {})
  public void testSearchNoResult() throws Exception {
    final Collection<SearchResult> res = new ArrayList<>();
    Assertions.assertFalse(findsResultsFor(res, "this text does not exists").call());
    Assertions.assertEquals(0, res.size(), "has result. none were expected");
  }

  @Test
  @WithKendra(indexName = "JSPWikiIndex", dataSourceName = "JSPWikiDataSource")
  @WithPage(name = "TestPage", text = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.", attachments = {})
  @WithResult(name = "TestPage", text = "mankind", scoreConfidence = ScoreConfidence.VERY_HIGH)
  public void testSimpleSearch() throws Exception {
    Collection<SearchResult> searchResults = new ArrayList<>();
    Assertions.assertTrue(findsResultsFor(searchResults, "mankind").call());
    Assertions.assertEquals(1, searchResults.size(), "no pages. one was expectd");
    Assertions.assertEquals("TestPage", searchResults.iterator().next().getPage().getName(), "the page TestPage was expected");
  }

  @Test
  @WithKendra(indexName = "JSPWikiIndex", dataSourceName = "JSPWikiDataSource")
  @WithPage(name = "TestPage", text = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.", attachments = {})
  @WithPage(name = "TestPage2", text = "It was the dawn of the third age of mankind, ten years after the Earth-Minbari War.", attachments = {})
  @WithResult(name = "TestPage", text = "mankind", scoreConfidence = ScoreConfidence.VERY_HIGH)
  @WithResult(name = "TestPage2", text = "mankind", scoreConfidence = ScoreConfidence.VERY_HIGH)
  public void testSimpleSearch2() throws Exception {
    Collection<SearchResult> searchResults = new ArrayList<>();
    Assertions.assertTrue(findsResultsFor(searchResults, "mankind").call());
    Assertions.assertEquals(2, searchResults.size(), "2 pages were expectd");
    Iterator<SearchResult> i = searchResults.iterator();
    Assertions.assertEquals("TestPage", i.next().getPage().getName(), "the page TestPage was expected");
    Assertions.assertEquals("TestPage2", i.next().getPage().getName(), "the page TestPage2 was expected");
  }

  private void setUpKendraMock(String indexName, String dataSourceName) throws Exception {
    final String indexId = UUID.randomUUID().toString();
    final String dataSourceId = UUID.randomUUID().toString();
    when(kendraMock.listIndices(any(ListIndicesRequest.class))).then(new Answer<ListIndicesResult>() {
      @Override
      public ListIndicesResult answer(InvocationOnMock invocation) throws Throwable {
        ListIndicesResult result = new ListIndicesResult();
        if (StringUtils.isNotBlank(indexName)) {
          result
              .withIndexConfigurationSummaryItems(new IndexConfigurationSummary().withId(indexId).withName(indexName));
        }
        return result;
      }
    });
    lenient().when(kendraMock.listDataSources(any(ListDataSourcesRequest.class)))
        .then(new Answer<ListDataSourcesResult>() {
          @Override
          public ListDataSourcesResult answer(InvocationOnMock invocation) throws Throwable {
            ListDataSourcesResult result = new ListDataSourcesResult();
            if (StringUtils.isNotBlank(dataSourceName)) {
              result.withSummaryItems(new DataSourceSummary().withId(dataSourceId).withName(dataSourceName));
            }
            return result;
          }
        });
    lenient().when(kendraMock.startDataSourceSyncJob(any(StartDataSourceSyncJobRequest.class)))
        .then(new Answer<StartDataSourceSyncJobResult>() {
          @Override
          public StartDataSourceSyncJobResult answer(InvocationOnMock invocation) throws Throwable {
            return new StartDataSourceSyncJobResult().withExecutionId("executionId");
          }
        });
    lenient().when(kendraMock.batchPutDocument(any(BatchPutDocumentRequest.class)))
        .then(new Answer<BatchPutDocumentResult>() {
          @Override
          public BatchPutDocumentResult answer(InvocationOnMock invocation) throws Throwable {
            BatchPutDocumentResult result = new BatchPutDocumentResult();
            result.withFailedDocuments(new ArrayList<>());
            return result;
          }
        });
    lenient().when(kendraMock.query(any(QueryRequest.class))).then(new Answer<QueryResult>() {
      @Override
      public QueryResult answer(InvocationOnMock invocation) throws Throwable {
        QueryResult result = new QueryResult();
        result.withResultItems(new ArrayList<>());
        return result;
      }
    });
  }

  private void addPages(final WithPage... withPages)
      throws WikiException, IOException, URISyntaxException {
    for (WithPage withPage : withPages ) {
      String name = withPage.name();
      String text =  withPage.text();
      String[] attachements = withPage.attachments();
      engine.saveText(name, text);
      ClassLoader classLoader = KendraSearchProviderTest.class.getClassLoader();
      for (String attachement : attachements) {
        byte[] content = Files.readAllBytes(Paths.get(classLoader.getResource(attachement).toURI()));
        engine.addAttachment(name, attachement, content);
      }
    }
  }

  private void addResults(final WithResult... withResults) {
    when(kendraMock.query(any(QueryRequest.class))).then(new Answer<QueryResult>() {
      @Override
      public QueryResult answer(InvocationOnMock invocation) throws Throwable {
        List<QueryResultItem> items = new ArrayList<>();
        for (WithResult withResult : withResults) {
          QueryResultItem item = new QueryResultItem().withType(QueryResultType.DOCUMENT);
          item.withDocumentId(withResult.name());
          item.withDocumentTitle(new TextWithHighlights().withText(withResult.name()));
          item.withDocumentExcerpt(new TextWithHighlights().withText(withResult.text()));
          item.withScoreAttributes(new ScoreAttributes().withScoreConfidence(withResult.scoreConfidence()));
          items.add(item);
        }
        return new QueryResult().withResultItems(items);
      }
    });
  }
}