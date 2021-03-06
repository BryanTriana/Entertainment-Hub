package com.google.ehub.servlets;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.ehub.data.EntertainmentItem;
import com.google.ehub.data.EntertainmentItemDatastore;
import com.google.ehub.data.EntertainmentItemList;
import com.google.gson.Gson;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(JUnit4.class)
public class DashboardServletTest {
  private static final String SEARCH_VALUE_PARAMETER_KEY = "searchValue";
  private static final String SORT_TYPE_PARAMETER_KEY = "sortType";
  private static final String JSON_CONTENT_TYPE = "application/json";

  private static final String ASCENDING_TITLE_PARAMETER_VALUE = "ASCENDING_TITLE";
  private static final String INVALID_SORT_PARAMETER_VALUE = "Invalid sort";

  private static final String ENTERTAINMENT_ITEM_KIND = "entertainmentItem";
  private static final String DISPLAY_TITLE_PROPERTY_KEY = "displayTitle";
  private static final String NORMALIZED_TITLE_PROPERTY_KEY = "normalizedTitle";
  private static final String DESCRIPTION_PROPERTY_KEY = "description";
  private static final String IMAGE_URL_PROPERTY_KEY = "imageUrl";
  private static final String RELEASE_DATE_PROPERTY_KEY = "releaseDate";
  private static final String RUNTIME_PROPERTY_KEY = "runtime";
  private static final String GENRE_PROPERTY_KEY = "genre";
  private static final String DIRECTORS_PROPERTY_KEY = "directors";
  private static final String WRITERS_PROPERTY_KEY = "writers";
  private static final String ACTORS_PROPERTY_KEY = "actors";
  private static final String OMDB_ID_PROPERTY_KEY = "omdbId";

  private static final String TITLE = "Star Wars";
  private static final String DESCRIPTION = "Blah....";
  private static final String IMAGE_URL = "Image.png";
  private static final String RELEASE_DATE = "09/26/1972";
  private static final String RUNTIME = "2 hours";
  private static final String GENRE = "Sci-Fi";
  private static final String DIRECTORS = "George Lucas";
  private static final String WRITERS = "George Lucas";
  private static final String ACTORS = "Mark Hamill, Harrison Ford";
  private static final String OMDB_ID = "tt23113212";

  private static final int PAGE_SIZE = 18;
  private static final int MAX_SEARCH_VALUE_CHARS = 150;

  private final DashboardServlet servlet = new DashboardServlet();
  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());

  @Mock HttpServletRequest request;
  @Mock HttpServletResponse response;
  @Mock PrintWriter printWriter;

  @Before
  public void init() {
    MockitoAnnotations.initMocks(this);
    helper.setUp();
  }

  @After
  public void tearDown() {
    helper.tearDown();
  }

  @Test
  public void getRequestWithNullParams_noContentGetsSent() throws IOException {
    when(request.getParameter(SEARCH_VALUE_PARAMETER_KEY)).thenReturn(null);
    when(request.getParameter(SORT_TYPE_PARAMETER_KEY)).thenReturn(null);
    when(response.getWriter()).thenReturn(printWriter);

    servlet.doGet(request, response);

    verify(response, never()).setContentType(JSON_CONTENT_TYPE);
  }

  @Test
  public void getRequestWithEmptyParams_noContentGetsSent() throws IOException {
    when(request.getParameter(SEARCH_VALUE_PARAMETER_KEY)).thenReturn("");
    when(request.getParameter(SORT_TYPE_PARAMETER_KEY)).thenReturn("");
    when(response.getWriter()).thenReturn(printWriter);

    servlet.doGet(request, response);

    verify(response, never()).setContentType(JSON_CONTENT_TYPE);
  }

  @Test
  public void getRequestWithValidParams_contentGetsSent() throws IOException {
    Entity itemEntity = new Entity(ENTERTAINMENT_ITEM_KIND);
    itemEntity.setProperty(DISPLAY_TITLE_PROPERTY_KEY, TITLE);
    itemEntity.setProperty(NORMALIZED_TITLE_PROPERTY_KEY, TITLE.toLowerCase());
    itemEntity.setProperty(DESCRIPTION_PROPERTY_KEY, DESCRIPTION);
    itemEntity.setProperty(IMAGE_URL_PROPERTY_KEY, IMAGE_URL);
    itemEntity.setProperty(RELEASE_DATE_PROPERTY_KEY, RELEASE_DATE);
    itemEntity.setProperty(RUNTIME_PROPERTY_KEY, RUNTIME);
    itemEntity.setProperty(GENRE_PROPERTY_KEY, GENRE);
    itemEntity.setProperty(DIRECTORS_PROPERTY_KEY, DIRECTORS);
    itemEntity.setProperty(WRITERS_PROPERTY_KEY, WRITERS);
    itemEntity.setProperty(ACTORS_PROPERTY_KEY, ACTORS);
    itemEntity.setProperty(OMDB_ID_PROPERTY_KEY, OMDB_ID);

    DatastoreService datastoreService = DatastoreServiceFactory.getDatastoreService();
    datastoreService.put(itemEntity);

    when(request.getParameter(SEARCH_VALUE_PARAMETER_KEY)).thenReturn(TITLE);
    when(request.getParameter(SORT_TYPE_PARAMETER_KEY)).thenReturn(ASCENDING_TITLE_PARAMETER_VALUE);
    when(response.getWriter()).thenReturn(printWriter);

    servlet.doGet(request, response);

    verify(response).setContentType(JSON_CONTENT_TYPE);
    verify(printWriter)
        .println(new Gson().toJson(EntertainmentItemDatastore.getInstance().queryItemsByTitlePrefix(
            FetchOptions.Builder.withLimit(PAGE_SIZE), TITLE, SortDirection.ASCENDING)));
  }

  @Test
  public void getRequestWithInvalidSortingParam_noContentGetsSent() throws IOException {
    when(request.getParameter(SEARCH_VALUE_PARAMETER_KEY)).thenReturn(TITLE);
    when(request.getParameter(SORT_TYPE_PARAMETER_KEY)).thenReturn(INVALID_SORT_PARAMETER_VALUE);
    when(response.getWriter()).thenReturn(printWriter);

    servlet.doGet(request, response);

    verify(response, never()).setContentType(JSON_CONTENT_TYPE);
  }

  @Test
  public void getRequestWithExceedingSearchValueLength_noContentGetsSent() throws IOException {
    when(request.getParameter(SEARCH_VALUE_PARAMETER_KEY))
        .thenReturn(getSearchValue(MAX_SEARCH_VALUE_CHARS + 1));
    when(request.getParameter(SORT_TYPE_PARAMETER_KEY)).thenReturn(ASCENDING_TITLE_PARAMETER_VALUE);
    when(response.getWriter()).thenReturn(printWriter);

    servlet.doGet(request, response);

    verify(response, never()).setContentType(JSON_CONTENT_TYPE);
  }

  @Test
  public void getRequestWithMaximumValidSearchValueLength_contentGetsSent() throws IOException {
    when(request.getParameter(SEARCH_VALUE_PARAMETER_KEY))
        .thenReturn(getSearchValue(MAX_SEARCH_VALUE_CHARS));
    when(request.getParameter(SORT_TYPE_PARAMETER_KEY)).thenReturn(ASCENDING_TITLE_PARAMETER_VALUE);
    when(response.getWriter()).thenReturn(printWriter);

    servlet.doGet(request, response);

    verify(response).setContentType(JSON_CONTENT_TYPE);
    verify(printWriter)
        .println(new Gson().toJson(EntertainmentItemDatastore.getInstance().queryItemsByTitlePrefix(
            FetchOptions.Builder.withLimit(PAGE_SIZE), /* Empty Search Value */ "",
            SortDirection.ASCENDING)));
  }

  private static String getSearchValue(int characterLength) {
    char[] searchValueChars = new char[characterLength];
    Arrays.fill(searchValueChars, 'a');
    return new String(searchValueChars);
  }
}
