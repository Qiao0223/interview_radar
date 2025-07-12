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
public class RawToStandardMapId implements Serializable {
    private static final long serialVersionUID = -344389062692547198L;
    @Column(name = "raw_question_id", nullable = false)
    private Long rawQuestionId;

    @Column(name = "standard_question_id", nullable = false)
    private Long standardQuestionId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        RawToStandardMapId entity = (RawToStandardMapId) o;
        return Objects.equals(this.standardQuestionId, entity.standardQuestionId) &&
                Objects.equals(this.rawQuestionId, entity.rawQuestionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(standardQuestionId, rawQuestionId);
    }

}