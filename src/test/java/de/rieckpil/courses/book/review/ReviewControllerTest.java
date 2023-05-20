package de.rieckpil.courses.book.review;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.HeaderResultMatchers;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static com.github.tomakehurst.wiremock.extension.responsetemplating.helpers.WireMockHelpers.jsonPath;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

  @MockBean
  private ReviewService reviewService;

  @Autowired
  private MockMvc mockMvc;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setup() {
    objectMapper = new ObjectMapper();
  }

  @Test
  void shouldReturnTwentyReviewsWithoutAnyOrderWhenNoParametersAreSpecified() throws Exception {

    ArrayNode result = objectMapper.createArrayNode();
    ObjectNode statistic = objectMapper.createObjectNode();
    statistic.put("bookId", 42);
    statistic.put("isbn", "42");
    statistic.put("avg", 89.3);
    statistic.put("ratings", 2);
    result.add(statistic);
    when(reviewService.getAllReviews(20, "none")).thenReturn(result);
    this.mockMvc
      .perform(MockMvcRequestBuilders.get("/api/books/reviews"))
      .andExpect(status().isOk())
      .andExpect(MockMvcResultMatchers.jsonPath("$.size()",Matchers.equalTo(1)));
  }

  @Test
  void shouldNotReturnReviewStatisticsWhenUserIsUnauthenticated() throws Exception {
    this.mockMvc
      .perform(MockMvcRequestBuilders.get("/api/books/reviews/statistics"))
      .andExpect(MockMvcResultMatchers.status().isUnauthorized());
  }

  @Test
 /* @WithMockUser(username = "duke") // Is a mocked user for spring security*/
  void shouldReturnReviewStatisticsWhenUserIsAuthenticated() throws Exception {

    this.mockMvc
      .perform(MockMvcRequestBuilders.get("/api/books/reviews/statistics")
        .with(jwt()))
      .andExpect(MockMvcResultMatchers.status().isOk());
    verify(reviewService,times(1)).getReviewStatistics();
  }

  @Test
  void shouldCreateNewBookReviewForAuthenticatedUserWithValidPayload() throws Exception {

    String review = """
      {
        "reviewTitle": "Good java book!",
        "reviewContent": "I really liked the book!",
        "rating": 4
      }
      """;
    when(reviewService.createBookReview(eq("42"), any(BookReviewRequest.class), eq("duke"), eq("duke@spring.io")))
      .thenReturn(84L);
    this.mockMvc
      .perform(MockMvcRequestBuilders.post("/api/books/{isbn}/reviews",42)
        .contentType(MediaType.APPLICATION_JSON)
        .content(review)
        .with(jwt().jwt(builder -> builder
          .claim("email", "duke@spring.io")
          .claim("preferred_username", "duke"))))
      .andExpect(MockMvcResultMatchers.status().isCreated())
      .andExpect(header().exists("Location"))
      .andExpect(header().string("Location", Matchers.containsString("/books/42/reviews/84")));
  }

  @Test
  void shouldRejectNewBookReviewForAuthenticatedUsersWithInvalidPayload() throws Exception {

    String review = """
      {
        "reviewTitle": "Good java book!",
        "reviewContent": "I really liked the book!",
        "rating": -1
      }
      """;
    when(reviewService.createBookReview(eq("42"), any(BookReviewRequest.class), eq("duke"), eq("duke@spring.io")))
      .thenReturn(84L);
    this.mockMvc
      .perform(MockMvcRequestBuilders.post("/api/books/{isbn}/reviews",42)
        .contentType(MediaType.APPLICATION_JSON)
        .content(review)
        .with(jwt().jwt(builder -> builder
          .claim("email", "duke@spring.io")
          .claim("preferred_username", "duke"))))
      .andExpect(MockMvcResultMatchers.status().isBadRequest())
      .andDo(print());
  }

  @Test
  void shouldNotAllowDeletingReviewsWhenUserIsAuthenticatedWithoutModeratorRole() throws Exception {
    this.mockMvc
      .perform(MockMvcRequestBuilders.delete("/api/books/{isbn}/reviews/{reviewId}", "42", "84")
        .with(jwt()))
      .andExpect(MockMvcResultMatchers.status().isForbidden());
    verifyNoInteractions(reviewService);
  }

  @Test
  /*@WithMockUser(roles = "moderator")*/
  void shouldAllowDeletingReviewsWhenUserIsAuthenticatedAndHasModeratorRole() throws Exception {
    this.mockMvc
      .perform(MockMvcRequestBuilders.delete("/api/books/{isbn}/reviews/{reviewId}", "42", "84")
        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_moderator"))))
      .andExpect(MockMvcResultMatchers.status().isOk());
    verify(reviewService).deleteReview("42",84L);
  }

}
