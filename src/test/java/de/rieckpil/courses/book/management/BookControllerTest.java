package de.rieckpil.courses.book.management;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(BookController.class)
class BookControllerTest {

  @MockBean
  private BookManagementService bookManagementService;

  @Autowired
  private MockMvc mockMvc;

  @Test
  void shouldGetEmptyArrayWhenNoBooksExists() throws Exception {
    this.mockMvc
      .perform(MockMvcRequestBuilders.get("/api/books")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(MockMvcResultMatchers.jsonPath("$.size()", Matchers.is(0)))
      .andDo(MockMvcResultHandlers.print());

  }

  @Test
  void shouldNotReturnXML() throws Exception {
    this.mockMvc
      .perform(MockMvcRequestBuilders.get("/api/books")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_XML))
      .andExpect(MockMvcResultMatchers.status().is(Matchers.is(406)))
      .andDo(print());
  }

  @Test
  void shouldGetBooksWhenServiceReturnsBooks() throws Exception {
    Book bookOne = createBook(1L,"42", "Java 14", "Mike", "Good Book",
      "Software Engineering", 200L, "Oracle", "ftp://localhost:42");
    Book bookTwo = createBook(1L,"42", "Java 15", "Duke", "Good Book",
      "Software Engineering", 200L, "Oracle", "ftp://localhost:42");
    when(bookManagementService.getAllBooks()).thenReturn(List.of(bookOne, bookTwo));
    this.mockMvc
      .perform(MockMvcRequestBuilders.get("/api/books")
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON))
      .andExpect(MockMvcResultMatchers.status().isOk())
      .andExpect(MockMvcResultMatchers.jsonPath("$.size()", Matchers.is(2)))
      .andExpect(MockMvcResultMatchers.jsonPath("$[0].isbn", Matchers.is("42")))
      .andExpect(MockMvcResultMatchers.jsonPath("$[0].title", Matchers.is("Java 14")))
      .andExpect(MockMvcResultMatchers.jsonPath("$[0].id").doesNotExist())
      .andExpect(MockMvcResultMatchers.jsonPath("$[1].isbn", Matchers.is("42")))
      .andExpect(MockMvcResultMatchers.jsonPath("$[1].title", Matchers.is("Java 15")))
      .andExpect(MockMvcResultMatchers.jsonPath("$[1].id").doesNotExist())
      .andDo(MockMvcResultHandlers.print());
  }

  private Book createBook(Long id,
                          String isbn,
                          String title,
                          String author,
                          String description,
                          String genre,
                          Long pages,
                          String publisher,
                          String thumbnailUrl) {

    Book result = new Book();
    result.setId(id);
    result.setIsbn(isbn);
    result.setTitle(title);
    result.setAuthor(author);
    result.setDescription(description);
    result.setGenre(genre);
    result.setPages(pages);
    result.setPublisher(publisher);
    result.setThumbnailUrl(thumbnailUrl);
    return result;
  }

}
