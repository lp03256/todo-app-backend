package com.todo.resource;

import com.todo.model.Todo;
import com.todo.model.TodoRequest;
import com.todo.repository.TodoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Objects;
import java.util.logging.Level;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.web.reactive.function.server.RequestPredicates.accept;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class TodoController {

    private final TodoRepository todoRepository;
    private final SecureRandom random = new SecureRandom();

    @Bean
    public RouterFunction<ServerResponse> todoControllerEndpoints() {
        return RouterFunctions.route()
                .GET( "/rest/v1/todos", accept(APPLICATION_JSON), serverRequest ->
                        todoRepository.findAll()
                                .log(this.getClass().getName(), Level.FINE)
                                .collectList()
                                .switchIfEmpty( Mono.error(new ResponseStatusException( HttpStatus.NO_CONTENT, "No Content available")))
                                .flatMap(results -> ServerResponse.ok().bodyValue(results))
                                .doOnError(throwable -> ServerResponse.badRequest().bodyValue(throwable.getMessage()))
                )
                .POST("/rest/v1/todos", accept(APPLICATION_JSON), serverRequest ->
                        serverRequest.bodyToMono( TodoRequest.class)
                                .log(this.getClass().getName(), Level.FINE)
                                .switchIfEmpty(Mono.error(new ServerWebInputException("data cannot be empty")))
                                .doOnError(throwable -> log.error("{}Todo data creation failure due to validation error: {}", serverRequest.exchange().getLogPrefix(), throwable.getMessage(), throwable))
                                .flatMap(next -> todoRepository.save(buildPushConfig(next))
                                        .doOnError(throwable -> log.error("{}Todo data failure creation failed due to persistence error: {}", serverRequest.exchange().getLogPrefix(), throwable.getMessage(), throwable))
                                        .retry(3)
                                )
                                .flatMap(todoConfig -> ServerResponse.status(HttpStatus.CREATED).bodyValue(
                                        new HashMap<String, String>() {{
                                            put( "id", todoConfig.getId() );
                                        }} ))
                )
                .PUT("/rest/v1/todos/{todoId}", accept(APPLICATION_JSON), serverRequest ->
                        serverRequest.bodyToMono(TodoRequest.class)
                                .log(this.getClass().getName(), Level.FINE)
                                .switchIfEmpty(Mono.error(new ServerWebInputException("cannot update todo with empty body")))
                                .doOnError(throwable -> log.error("{}Todo {} update failed due to validation error {}", serverRequest.exchange().getLogPrefix(), serverRequest.pathVariable("todoId"), throwable.getMessage(), throwable))
                                .flatMap(todoRequest -> todoRepository.findBytodoId(serverRequest.pathVariable("todoId"))
                                                            .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, serverRequest.pathVariable("todoId") + "  not found")))
                                                            .doOnError(throwable -> log.error("{}Failed processing update for todo ID {} due to error {}", serverRequest.exchange().getLogPrefix(), serverRequest.pathVariable("todoId"), throwable.getMessage(), throwable))
                                                            .flatMap(todoConfig -> {
                                                                todoConfig.setTask( todoRequest.getTask() );
                                                                todoConfig.setCompleted( todoRequest.getCompleted() );
                                                                todoConfig.setIsEditing( todoRequest.getIsEditing() );
                                                                return todoRepository.save(todoConfig);
                                                            })
                                                            .doOnError(throwable -> log.error("{}Failed processing update for todo ID {} due to error {}", serverRequest.exchange().getLogPrefix(), serverRequest.pathVariable("todoId"), throwable.getMessage(), throwable)))
                                .flatMap(todoConfig -> ServerResponse.status(HttpStatus.OK).bodyValue(todoConfig))
                )
                .DELETE( "/rest/v1/todos/{todoId}", accept(APPLICATION_JSON), serverRequest ->
                                todoRepository.findBytodoId(serverRequest.pathVariable("todoId"))
                                        .log(this.getClass().getName(), Level.FINE)
                                        .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                                        .doOnError(throwable -> log.error("{}Failed to find todo {} for deletion due to error {}", serverRequest.exchange().getLogPrefix(), serverRequest.pathVariable("todoId"), throwable.getMessage(), throwable))
                                        .flatMap(todo -> todoRepository.delete(todo)
                                                .doOnError(throwable -> log.error("{}Failed deleting todo {} due to error {}", serverRequest.exchange().getLogPrefix(), serverRequest.pathVariable("todoId"), throwable.getMessage(), throwable)))
                                        .then(ServerResponse.status(HttpStatus.OK).build())
                        )
                .build();
    }

    private Todo buildPushConfig ( @NotNull TodoRequest todoRequest) {
        return   Todo.builder()
                .id(String.join("-", "T", String.valueOf(random.nextLong())) )
                .task( todoRequest.getTask() )
                .completed( todoRequest.getCompleted() )
                .isEditing( todoRequest.getIsEditing() )
                .build();
    }
}
