package com.todo.repository;

import com.todo.model.Todo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

public interface TodoRepository extends ReactiveMongoRepository<Todo, Integer> {
    Mono<Todo> findBytodoId( String id);
}
