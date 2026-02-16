package com.example.shajara.entity;


import com.example.shajara.enums.RelationType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "relations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Relation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_person_id", nullable = false)
    private Person fromPerson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_person_id", nullable = false)
    private Person toPerson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RelationType type;

    // Faqat SPOUSE uchun ahamiyatli
    @Column(nullable = false, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean divorced = false;

    // Optional: biznes mantiqni yordamchi metod orqali qo'llash
    public boolean isDivorced() {
        return this.type == RelationType.SPOUSE && divorced;
    }

    public void setDivorced(boolean divorced) {
        if(this.type == RelationType.SPOUSE) {
            this.divorced = divorced;
        }
    }


    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof Relation)) return false;
        return id != null && id.equals(((Relation)o).getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

}