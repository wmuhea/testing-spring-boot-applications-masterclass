package de.rieckpil.courses.book.management;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static reactor.netty.tcp.TcpClient.create;

public class PeterOpenLibraryApiClientTest {

  private static final String ISBN = "9780596004651";

  private static String VALID_RESPONSE;

  static {
    try {
      VALID_RESPONSE = new String(PeterOpenLibraryApiClientTest.class
        .getClassLoader()
        .getResourceAsStream("stubs/openlibrary/success-" + ISBN + ".json")
        .readAllBytes());
    } catch (IOException ex) {
      ex.printStackTrace();
    }
  }

  private MockWebServer mockWebServer;
  private OpenLibraryApiClient cut;     // class under test

  @BeforeEach
  void beforeEach() throws IOException {
    Logger.getLogger(MockWebServer.class.getName()).setLevel(Level.FINEST);

    mockWebServer = new MockWebServer();
    mockWebServer.start();

    HttpClient httpClient = HttpClient.create()
      .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2_000)
      .doOnConnected(connection ->
        connection.addHandlerLast(new ReadTimeoutHandler(2))
          .addHandlerLast(new WriteTimeoutHandler(2)));

    this.cut = new OpenLibraryApiClient(
      WebClient
        .builder()
        .clientConnector(new ReactorClientHttpConnector(httpClient))
        .baseUrl(mockWebServer.url("/").toString())
        .build()
    );
  }

  @Test
  void notNull() {
    assertNotNull(mockWebServer);
    assertNotNull(cut);
  }

  @Test
  void shouldReturnBookWhenResultIsSuccess() throws InterruptedException {

    MockResponse mockResponse = new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setBody(VALID_RESPONSE)
      .setResponseCode(HttpStatus.OK.value());

    this.mockWebServer.enqueue(mockResponse);

    Book result = cut.fetchMetadataForBook(ISBN);

    assertNotNull(result);
    assertEquals("9780596004651", result.getIsbn());
    assertEquals("Head first Java", result.getTitle());
    assertEquals("O'Reilly", result.getPublisher());
    assertEquals("Kathy Sierra", result.getAuthor());
    assertEquals("Your brain on Java--a learner's guide--Cover.Includes index.", result.getDescription());
    assertEquals(619L, result.getPages());
    assertEquals("Java (Computer program language)", result.getGenre());
    assertEquals("https://covers.openlibrary.org/b/id/388761-S.jpg", result.getThumbnailUrl());

    assertNull(result.getId());

    RecordedRequest recordedRequest = this.mockWebServer.takeRequest();

    assertEquals("/api/books?jscmd=data&format=json&bibkeys=ISBN:" + ISBN, recordedRequest.getPath());
  }

  @Test
  void shouldReturnBookWhenResultIsSuccessfulButLackingAllInformation() {


    String response = """
        {
          "ISBN:9780596004651": {
              "publishers": [
                {
                  "name": "O'Reilly"
                }
              ],
              "title": "Head second Java",
              "number_of_pages": 42,
              "cover": {
                "small": "https://covers.openlibrary.org/b/id/388761-S.jpg",
                "large": "https://covers.openlibrary.org/b/id/388761-L.jpg",
                "medium": "https://covers.openlibrary.org/b/id/388761-M.jpg"
              },
              "authors": [
                {
                  "url": "https://openlibrary.org/authors/OL1400543A/Kathy_Sierra",
                  "name": "Kathy Sierra"
                }
              ],
          }
        }
      """;



    mockWebServer.enqueue( new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setResponseCode(HttpStatus.OK.value())
      .setBody(response.replace("\n", "")));


    Book result = cut.fetchMetadataForBook(ISBN);

    assertNotNull(result);
    assertEquals("9780596004651", result.getIsbn());
    assertEquals("Head second Java", result.getTitle());
    assertEquals("O'Reilly", result.getPublisher());
    assertEquals("Kathy Sierra", result.getAuthor());
    assertEquals(42L, result.getPages());
    assertEquals("n.A", result.getDescription());
    assertEquals("n.A", result.getGenre());
    assertEquals("https://covers.openlibrary.org/b/id/388761-S.jpg", result.getThumbnailUrl());

    assertNull(result.getId());
  }

  @Test
  void shouldPropagateExceptionWhenRemoteSystemIsDown() {
    assertThrows(RuntimeException.class, () -> {
      this.mockWebServer.enqueue(
        new MockResponse()
          .setResponseCode(500)
        .setBody("Sorry, system is down :(")
      );
      cut.fetchMetadataForBook(ISBN);
    });
  }

  @Test
  void shouldRetryWhenRemoteSystemIsSlowOrFailing() {

    this.mockWebServer.enqueue(
      new MockResponse()
        .setResponseCode(500)
        .setBody("Sorry, system is down :(")
    );
    this.mockWebServer.enqueue(new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setBody(VALID_RESPONSE)
      .setResponseCode(HttpStatus.OK.value())
      .setBodyDelay(2, TimeUnit.SECONDS)
    );
    this.mockWebServer.enqueue(new MockResponse()
      .addHeader("Content-Type", "application/json; charset=utf-8")
      .setBody(VALID_RESPONSE)
      .setResponseCode(HttpStatus.OK.value())
    );

    Book result = cut.fetchMetadataForBook(ISBN);

    assertNotNull(result);
    assertEquals("9780596004651", result.getIsbn());
    assertNull(result.getId());

  }
}
