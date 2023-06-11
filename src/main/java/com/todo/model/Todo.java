package com.todo.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;

@Getter
@Setter
@FieldNameConstants
@ToString
@Builder
@Document(collection = "todo_data")
public class Todo implements Serializable {

    @Indexed(unique = true)
    private String id;

    private String task;

    private Boolean completed;

    private Boolean isEditing;
}
