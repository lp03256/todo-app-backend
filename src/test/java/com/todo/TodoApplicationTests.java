package com.todo;

import com.todo.model.Todo;
import com.todo.model.TodoRequest;
import com.todo.repository.TodoRepository;
import com.todo.resource.TodoController;
import lombok.extern.slf4j.Slf4j;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.BDDMockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.HashMap;

@Slf4j
@WebFluxTest(properties = {
		"spring.main.banner-mode=off",
		"logging.level.root=DEBUG",
		"server.error.include-message=always",
		"spring.application.name=todo"
})
@ContextConfiguration(classes = { TodoController.class })
@DisplayName("Todo REST API")
class TodoApplicationTests {

	public static final String BASE_URL = "/rest/v1/todos";

	@Autowired
	WebTestClient webClient;

	MultiValueMap<String, String> headers;

	@MockBean
	TodoRepository repository;

	@BeforeEach
	void setUp() {
		headers = new HttpHeaders();
		headers.setAll(new HashMap<String, String>() {{
			put( HttpHeaders.CONTENT_TYPE,  MediaType.APPLICATION_JSON_VALUE);
		}});
	}

	@Test
	@DisplayName("Successful creation of Todo")
	void testCreateTodo() {
		TodoRequest request = new TodoRequest( "have breakfast", false, false );

		BDDMockito.when(repository.save( ArgumentMatchers.any( Todo.class)))
				.thenReturn(
						Mono.just( Todo.builder()
								.id( "T-1233" )
								.task( request.getTask() )
								.completed( request.getCompleted() )
								.isEditing( request.getIsEditing() )
								.build()
						));

		webClient.post()
				.uri( URI.create(BASE_URL))
				.contentType(MediaType.APPLICATION_JSON)
				.headers(httpHeaders -> httpHeaders.addAll(headers))
				.bodyValue(request)
				.exchange()
				.expectStatus().isCreated()
				.expectBody().jsonPath("$.id", IsEqual.equalTo("T-1233"));
	}

	@Test
	@DisplayName("Get Todo")
	void testGetTodo() {

		BDDMockito.when(repository.findAll())
				.thenReturn(
						Flux.just( Todo.builder()
								.id( "T-1233" )
								.task( "abcd" )
								.completed( false )
								.isEditing( false )
								.build()
						));

		webClient.get()
				.uri( URI.create(BASE_URL))
				.headers(httpHeaders -> httpHeaders.addAll(headers))
				.exchange()
				.expectStatus().isOk();
	}

	@Test
	@DisplayName("Updation of Todo")
	void testUpdateTodo() {

		TodoRequest request = new TodoRequest( "have breakfast", false, false );

		BDDMockito.when(repository.findBytodoId("T-1233"))
				.thenReturn(
						Mono.just( Todo.builder()
								.id( "T-1233" )
								.task( "abcd" )
								.completed( false )
								.isEditing( false )
								.build()
						));

		BDDMockito.when(repository.save( ArgumentMatchers.any( Todo.class)))
				.thenReturn(
						Mono.just( Todo.builder()
								.id( "T-1233" )
								.task( request.getTask() )
								.completed( request.getCompleted() )
								.isEditing( request.getIsEditing() )
								.build()
						));

		webClient.put()
				.uri( URI.create(BASE_URL+"/T-1233"))
				.contentType(MediaType.APPLICATION_JSON)
				.headers(httpHeaders -> httpHeaders.addAll(headers))
				.bodyValue(request)
				.exchange()
				.expectStatus().isOk()
				;
	}

	@Test
	@DisplayName("Deletion of Todo")
	void testDeletionTodo() {

		BDDMockito.when(repository.findBytodoId("T-1233"))
				.thenReturn(
						Mono.just( Todo.builder()
								.id( "T-1233" )
								.task( "abcd" )
								.completed( false )
								.isEditing( false )
								.build()
						));

		BDDMockito.when(repository.delete( ArgumentMatchers.any( Todo.class))).thenReturn( Mono.empty() );

		webClient.delete()
				.uri( URI.create(BASE_URL+"/T-1233"))
				.headers(httpHeaders -> httpHeaders.addAll(headers))
				.exchange()
				.expectStatus().isOk()
		;
	}
}
