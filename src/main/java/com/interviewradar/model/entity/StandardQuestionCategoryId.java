package com.interviewradar.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
@Embeddable
public class StandardQuestionCategoryId implements Serializable {
    private static final long serialVersionUID = -5614778622345254890L;
    @Column(name = "standard_question_id", nullable = false)
    private Long standardQuestionId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        StandardQuestionCategoryId entity = (StandardQuestionCategoryId) o;
        return Objects.equals(this.standardQuestionId, entity.standardQuestionId) &&
                Objects.equals(this.categoryId, entity.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(standardQuestionId, categoryId);
    }

}