package com.todo.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@ToString
@AllArgsConstructor
@Builder
@JsonDeserialize(builder = TodoRequest.TodoRequestBuilder.class)
public class TodoRequest {

    @JsonProperty
    private String task;
    @JsonProperty
    private Boolean completed;
    @JsonProperty
    private Boolean isEditing;

}
