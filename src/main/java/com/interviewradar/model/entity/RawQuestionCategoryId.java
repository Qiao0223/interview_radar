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
public class RawQuestionCategoryId implements Serializable {
    private static final long serialVersionUID = 4577846109364163882L;
    @Column(name = "raw_question_id", nullable = false)
    private Long rawQuestionId;

    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        RawQuestionCategoryId entity = (RawQuestionCategoryId) o;
        return Objects.equals(this.rawQuestionId, entity.rawQuestionId) &&
                Objects.equals(this.categoryId, entity.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rawQuestionId, categoryId);
    }

}