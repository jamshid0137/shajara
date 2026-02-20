package com.example.shajara.repository;


import com.example.shajara.entity.Person;
import com.example.shajara.entity.Relation;
import com.example.shajara.enums.RelationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import java.util.List;
import java.util.Optional;

@Repository
public interface RelationRepository extends JpaRepository<Relation, Long> {
    List<Relation> findAllByFromPersonId(Long fromPersonId);

    List<Relation> findAllByToPersonId(Long toPersonId);

    List<Relation> findAllByType(RelationType type);

    @Query("SELECT r FROM Relation r WHERE r.fromPerson.familyTree.id = :treeId OR r.toPerson.familyTree.id = :treeId")
    List<Relation> findAllByTreeId(Long treeId);

    @Query("SELECT r.toPerson FROM Relation r WHERE r.fromPerson.id = :id AND r.type = 'PARENT'")
    List<Person> findChildren(Long id);

    @Query("""
        SELECT CASE WHEN r.fromPerson.id = :id THEN r.toPerson ELSE r.fromPerson END
        FROM Relation r
        WHERE r.type = 'SPOUSE' AND (r.fromPerson.id = :id OR r.toPerson.id = :id)
        """)
    List<Person> findSpouses(Long id);



    @Query("""
    select r from Relation r
    where r.type = :type and
    (
        (r.fromPerson.id = :p1 and r.toPerson.id = :p2)
        or
        (r.fromPerson.id = :p2 and r.toPerson.id = :p1)
    )
""")
    List<Relation> findSpouseRelationsBetween(
            @Param("p1") Long p1,
            @Param("p2") Long p2,
            @Param("type") RelationType type
    );




    @Query(value = """
SELECT * FROM persons p
WHERE p.id IN (

    -- 1) fromPerson X bo'lsa va type = SPOUSE, ajrashmagan
    SELECT r.to_person_id FROM relations r
    WHERE r.from_person_id = :personId
      AND r.type = 'SPOUSE'

    UNION

    -- 2) toPerson X bo'lsa va type = SPOUSE, ajrashmagan
    SELECT r.from_person_id FROM relations r
    WHERE r.to_person_id = :personId
      AND r.type = 'SPOUSE'
)
""", nativeQuery = true)
    List<Person> findAllSpousesNative(@Param("personId") Long personId);





    @Query(value = """
SELECT r.id FROM relations r
WHERE r.from_person_id=:personId OR r.to_person_id=:personId
""", nativeQuery = true)
    List<Long> findForAllSpousesNativeRelationdIds(@Param("personId") Long personId);



    // FamilyTree bo'yicha relationlarni olish
    @Query("SELECT r FROM Relation r " +
            "WHERE r.fromPerson.familyTree.id = :treeId " +
            "   OR r.toPerson.familyTree.id = :treeId")
    List<Relation> findByTreeId(@Param("treeId") Long treeId);
}
