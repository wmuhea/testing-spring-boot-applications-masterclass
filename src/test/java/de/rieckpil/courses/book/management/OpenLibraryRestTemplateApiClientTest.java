package de.rieckpil.courses.book.management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.test.web.client.match.MockRestRequestMatchers;
import org.springframework.test.web.client.response.MockRestResponseCreators;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.client.HttpServerErrorException;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RestClientTest(OpenLibraryRestTemplateApiClient.class)
class OpenLibraryRestTemplateApiClientTest {

  @Autowired
  private OpenLibraryRestTemplateApiClient cut;

  @Autowired
  private MockRestServiceServer mockRestServiceServer;

  private static final String ISBN = "9780596004651";

  @Test
  void shouldInjectBeans() {
    assertNotNull(mockRestServiceServer);
    assertNotNull(cut);
  }

  @Test
  void shouldReturnBookWhenResultIsSuccess() {
    this.mockRestServiceServer
      .expect(MockRestRequestMatchers.requestTo(Matchers.containsString(ISBN)))
      .andRespond(withSuccess(new ClassPathResource("/stubs/openlibrary/success-"+ISBN+".json"), MediaType.APPLICATION_JSON));
    Book result = cut.fetchMetadataForBook(ISBN);
    assertEquals("Head first Java", result.getTitle());
    assertEquals(ISBN, result.getIsbn());
    assertEquals("Kathy Sierra", result.getAuthor());
    assertEquals("O'Reilly", result.getPublisher());
    assertEquals(619L, result.getPages());
    assertEquals("Your brain on Java--a learner's guide--Cover.Includes index.", result.getDescription());
    assertEquals("Java (Computer program language)", result.getGenre());
    assertEquals("https://covers.openlibrary.org/b/id/388761-S.jpg", result.getThumbnailUrl());
    assertNull(result.getId());
  }

  @Test
  void shouldReturnBookWhenResultIsSuccessButLackingAllInformation() {
    String response = """
      {
        "9780596004651": {
          "publishers": [
              {
                "name": "O'Reilly"
              }
          ],
          "title": "Head first Java",
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
          ]
        }
      }

      """;
    this.mockRestServiceServer
      .expect(requestTo(Matchers.containsString("/api/books")))
      .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

    Book result = cut.fetchMetadataForBook(ISBN);
    assertEquals("Head first Java", result.getTitle());
    assertEquals(ISBN, result.getIsbn());
    assertEquals("Kathy Sierra", result.getAuthor());
    assertEquals("O'Reilly", result.getPublisher());
    assertEquals("n.A", result.getDescription());
    assertEquals("n.A", result.getGenre());
    assertEquals(42, result.getPages());
    assertEquals("https://covers.openlibrary.org/b/id/388761-S.jpg", result.getThumbnailUrl());
    this.mockRestServiceServer.verify(); // helps to check the url call are actually made to the mocked server

  }

  @Test
  void shouldPropagateExceptionWhenRemoteSystemIsDown() {
    assertThrows(HttpServerErrorException.class, () -> {
      this.mockRestServiceServer
        .expect(requestTo(Matchers.containsString(ISBN)))
        .andRespond(withServerError());
      cut.fetchMetadataForBook(ISBN);
    });

  }

  @Test
  // This is for investigating headers
  void shouldContainCorrectHeadersWhenRemoteSystemIsInvoked() {
    /*this.mockRestServiceServer
      .expect(requestTo(Matchers.containsString(ISBN)))
      .andExpect(header("ene", "negn"))
      .andExpect(header("sew", "ale"))
      .andRespond(withServerError());*/
  }
}
