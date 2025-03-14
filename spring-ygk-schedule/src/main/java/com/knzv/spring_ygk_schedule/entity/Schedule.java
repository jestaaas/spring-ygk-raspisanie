package com.knzv.spring_ygk_schedule.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.util.Objects;

@Getter
@Setter
@Entity
@Table(name = "schedule")
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Schedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
    @Column(nullable = false)
    private int pairNumber;
    @Column(nullable = false)
    private String subject;
    @Column(nullable = false)
    private String teacher;
    @Column(nullable = false)
    private String room;

    @ManyToOne
    @JoinColumn(name = "weekday_id")
    private WeekDay weekDay;
    @ManyToOne
    @JoinColumn(name = "weektype_id")
    private WeekType weekType;
    @ManyToOne
    @JoinColumn(name = "group_id")
    private Group group;

    @Override
    public final boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        Class<?> oEffectiveClass = o instanceof HibernateProxy ? ((HibernateProxy) o).getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != oEffectiveClass) return false;
        Schedule schedule = (Schedule) o;
        return getId() != null && Objects.equals(getId(), schedule.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy ? ((HibernateProxy) this).getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}