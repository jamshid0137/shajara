package com.example.shajara.service;

import com.example.shajara.dto.layoutnew.TreeLayoutResponseDtoNew;
import com.example.shajara.dto.layoutnew.TreeNodeDtoNew;
import com.example.shajara.entity.Person;
import com.example.shajara.exception.NotFoundException;
import com.example.shajara.repository.PersonRepository;
import com.example.shajara.repository.RelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Shajara layout algoritmi.
 *
 * Bu service faqat nodelar va connectionlarni qaytaradi.
 * X va Y koordinatalar null — ularni frontend (OrgChartLayoutCalculatorNew) hisoblaydi.
 *
 * Ma'lumotlar tuzilmasi:
 * - Person.fatherId / Person.motherId → ota-ona
 * - Relation (type=SPOUSE) → turmush o'rtoqlari
 * - findAllByFatherIdOrMotherId() → bolalar
 */
@Service
@RequiredArgsConstructor
public class TreeLayoutServiceNew {

    private final PersonRepository personRepository;
    private final RelationRepository relationRepository;

    // =========================================================
    // MARKAZIY SHAXS ATROFIDAGI LAYOUT
    // GET /api/layoutnew/person/{personId}
    // =========================================================
    @Transactional(readOnly = true)
    public TreeLayoutResponseDtoNew calculateLayout(Long personId) {
        Person center = personRepository.findById(personId)
                .orElseThrow(() -> new NotFoundException("Person not found: " + personId));

        Map<Long, TreeNodeDtoNew> nodeMap = new LinkedHashMap<>();
        List<TreeLayoutResponseDtoNew.ConnectionDto> connections = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        // 1. MARKAZ
        addNode(nodeMap, visited, center, "CENTER");

        // 2. OTA-ONALAR
        placeParents(center, nodeMap, connections, visited);

        // 3. TURMUSH O'RTOQLARI
        placeSpouses(center, nodeMap, connections, visited);

        // 4. AKA-UKALAR
        placeSiblings(center, nodeMap, connections, visited);

        // 5. BOLALAR
        placeChildren(center, nodeMap, connections, visited);

        return TreeLayoutResponseDtoNew.builder()
                .nodes(new ArrayList<>(nodeMap.values()))
                .connections(connections)
                .minX(0).maxX(0)
                .minY(0).maxY(0)
                .build();
    }

    // =========================================================
    // BUTUN DARAXTNI QAYTARISH — Barcha persons
    // GET /api/layoutnew/tree/{treeId}
    // FamilyTree.js OrgChartLayoutCalculator koordinatalarni o'zi hisoblaydi.
    // =========================================================
    @Transactional(readOnly = true)
    public TreeLayoutResponseDtoNew calculateFullTreeLayout(Long treeId) {
        List<Person> allPersons = personRepository.findAllByFamilyTreeId(treeId);

        Map<Long, TreeNodeDtoNew> nodeMap = new LinkedHashMap<>();
        List<TreeLayoutResponseDtoNew.ConnectionDto> connections = new ArrayList<>();

        for (Person p : allPersons) {
            nodeMap.put(p.getId(), TreeNodeDtoNew.builder()
                    .id(p.getId())
                    .name(p.getName())
                    .gender(p.getGender() != null ? p.getGender().name() : null)
                    .role(null)
                    .birthDate(p.getBirthDate())
                    .diedDate(p.getDiedDate())
                    .photoUrl(p.getPhotoUrl())
                    .treeId(p.getFamilyTree() != null ? p.getFamilyTree().getId() : null)
                    .fatherId(p.getFatherId())
                    .motherId(p.getMotherId())
                    .x(null)
                    .y(null)
                    .build());

            // Parent connections
            if (p.getFatherId() != null) {
                connections.add(conn(p.getFatherId(), p.getId(), "PARENT_CHILD"));
            }
            if (p.getMotherId() != null) {
                connections.add(conn(p.getMotherId(), p.getId(), "PARENT_CHILD"));
            }

            // Spouse connections
            List<Person> spouses = relationRepository.findAllSpousesNative(p.getId());
            for (Person sp : spouses) {
                if (p.getId() < sp.getId()) {
                    connections.add(conn(p.getId(), sp.getId(), "SPOUSE"));
                }
            }
        }

        return TreeLayoutResponseDtoNew.builder()
                .nodes(new ArrayList<>(nodeMap.values()))
                .connections(connections)
                .minX(0).maxX(0).minY(0).maxY(0)
                .build();
    }

    // =========================================================
    // OTA-ONALAR
    // =========================================================
    private void placeParents(Person center,
            Map<Long, TreeNodeDtoNew> nodeMap,
            List<TreeLayoutResponseDtoNew.ConnectionDto> connections,
            Set<Long> visited) {

        if (center.getFatherId() != null) {
            personRepository.findById(center.getFatherId()).ifPresent(father -> {
                if (!visited.contains(father.getId())) {
                    addNode(nodeMap, visited, father, "FATHER");
                    connections.add(conn(father.getId(), center.getId(), "PARENT_CHILD"));

                    // Buvasi (otaning otasi)
                    if (father.getFatherId() != null) {
                        personRepository.findById(father.getFatherId()).ifPresent(gf -> {
                            if (!visited.contains(gf.getId())) {
                                addNode(nodeMap, visited, gf, "FATHER");
                                connections.add(conn(gf.getId(), father.getId(), "PARENT_CHILD"));
                            }
                        });
                    }
                    // Buvisi (otaning onasi)
                    if (father.getMotherId() != null) {
                        personRepository.findById(father.getMotherId()).ifPresent(gm -> {
                            if (!visited.contains(gm.getId())) {
                                addNode(nodeMap, visited, gm, "MOTHER");
                                connections.add(conn(gm.getId(), father.getId(), "PARENT_CHILD"));
                                if (father.getFatherId() != null) {
                                    connections.add(conn(father.getFatherId(), gm.getId(), "SPOUSE"));
                                }
                            }
                        });
                    }
                }
            });
        }

        if (center.getMotherId() != null) {
            personRepository.findById(center.getMotherId()).ifPresent(mother -> {
                if (!visited.contains(mother.getId())) {
                    addNode(nodeMap, visited, mother, "MOTHER");
                    connections.add(conn(mother.getId(), center.getId(), "PARENT_CHILD"));

                    if (center.getFatherId() != null) {
                        connections.add(conn(center.getFatherId(), mother.getId(), "SPOUSE"));
                    }

                    // Buvasi (onaning otasi)
                    if (mother.getFatherId() != null) {
                        personRepository.findById(mother.getFatherId()).ifPresent(gf -> {
                            if (!visited.contains(gf.getId())) {
                                addNode(nodeMap, visited, gf, "FATHER");
                                connections.add(conn(gf.getId(), mother.getId(), "PARENT_CHILD"));
                            }
                        });
                    }
                    // Buvisi (onaning onasi)
                    if (mother.getMotherId() != null) {
                        personRepository.findById(mother.getMotherId()).ifPresent(gm -> {
                            if (!visited.contains(gm.getId())) {
                                addNode(nodeMap, visited, gm, "MOTHER");
                                connections.add(conn(gm.getId(), mother.getId(), "PARENT_CHILD"));
                                if (mother.getFatherId() != null) {
                                    connections.add(conn(mother.getFatherId(), gm.getId(), "SPOUSE"));
                                }
                            }
                        });
                    }
                }
            });
        }
    }

    // =========================================================
    // TURMUSH O'RTOQLARI
    // =========================================================
    private void placeSpouses(Person center,
            Map<Long, TreeNodeDtoNew> nodeMap,
            List<TreeLayoutResponseDtoNew.ConnectionDto> connections,
            Set<Long> visited) {
        List<Person> spouses = relationRepository.findAllSpousesNative(center.getId());
        if (spouses == null || spouses.isEmpty()) return;

        for (Person spouse : spouses) {
            if (visited.contains(spouse.getId())) continue;

            addNode(nodeMap, visited, spouse, "SPOUSE");
            connections.add(conn(center.getId(), spouse.getId(), "SPOUSE"));

            // Spousening ota-onasi
            if (spouse.getFatherId() != null) {
                personRepository.findById(spouse.getFatherId()).ifPresent(father -> {
                    if (!visited.contains(father.getId())) {
                        addNode(nodeMap, visited, father, "FATHER");
                        connections.add(conn(father.getId(), spouse.getId(), "PARENT_CHILD"));
                    }
                });
            }
            if (spouse.getMotherId() != null) {
                personRepository.findById(spouse.getMotherId()).ifPresent(mother -> {
                    if (!visited.contains(mother.getId())) {
                        addNode(nodeMap, visited, mother, "MOTHER");
                        connections.add(conn(mother.getId(), spouse.getId(), "PARENT_CHILD"));
                        if (spouse.getFatherId() != null) {
                            connections.add(conn(spouse.getFatherId(), mother.getId(), "SPOUSE"));
                        }
                    }
                });
            }

            // Spousening o'z (boshqa) farzandlari
            List<Person> spouseChildren = personRepository.findAllByFatherIdOrMotherId(spouse.getId(), spouse.getId());
            for (Person spChild : spouseChildren) {
                if (!visited.contains(spChild.getId())) {
                    Long otherParentId = null;
                    if (spChild.getFatherId() != null && !spChild.getFatherId().equals(spouse.getId())) {
                        otherParentId = spChild.getFatherId();
                    } else if (spChild.getMotherId() != null && !spChild.getMotherId().equals(spouse.getId())) {
                        otherParentId = spChild.getMotherId();
                    }

                    // Bola boshqa spouse bilan bo'lsa (markaz bilan emas) — yashiramiz
                    if (otherParentId != null && !otherParentId.equals(center.getId())) {
                        continue;
                    }

                    addNode(nodeMap, visited, spChild, "CHILD");
                    connections.add(conn(spouse.getId(), spChild.getId(), "PARENT_CHILD"));

                    if (spChild.getFatherId() != null && !spChild.getFatherId().equals(spouse.getId())
                            && visited.contains(spChild.getFatherId())) {
                        connections.add(conn(spChild.getFatherId(), spChild.getId(), "PARENT_CHILD"));
                    }
                    if (spChild.getMotherId() != null && !spChild.getMotherId().equals(spouse.getId())
                            && visited.contains(spChild.getMotherId())) {
                        connections.add(conn(spChild.getMotherId(), spChild.getId(), "PARENT_CHILD"));
                    }
                }
            }
        }
    }

    // =========================================================
    // AKA-UKALAR
    // =========================================================
    private void placeSiblings(Person center,
            Map<Long, TreeNodeDtoNew> nodeMap,
            List<TreeLayoutResponseDtoNew.ConnectionDto> connections,
            Set<Long> visited) {
        if (center.getFatherId() == null && center.getMotherId() == null) return;

        Long fId = center.getFatherId() != null ? center.getFatherId() : -1L;
        Long mId = center.getMotherId() != null ? center.getMotherId() : -1L;
        List<Person> siblings = personRepository.findAllByFatherIdOrMotherId(fId, mId);

        for (Person s : siblings) {
            if (s.getId().equals(center.getId()) || visited.contains(s.getId())) continue;

            addNode(nodeMap, visited, s, "SIBLING");

            if (center.getFatherId() != null) {
                connections.add(conn(center.getFatherId(), s.getId(), "PARENT_CHILD"));
            } else if (center.getMotherId() != null) {
                connections.add(conn(center.getMotherId(), s.getId(), "PARENT_CHILD"));
            }

            // Aka-ukaning spouselari
            List<Person> siblingSpouses = relationRepository.findAllSpousesNative(s.getId());
            for (Person sbSp : siblingSpouses) {
                if (!visited.contains(sbSp.getId())) {
                    addNode(nodeMap, visited, sbSp, "SPOUSE");
                    connections.add(conn(s.getId(), sbSp.getId(), "SPOUSE"));
                }
            }

            // Aka-ukaning farzandlari
            List<Person> siblingChildren = personRepository.findAllByFatherIdOrMotherId(s.getId(), s.getId());
            for (Person sc : siblingChildren) {
                if (!visited.contains(sc.getId())) {
                    Long otherParentId = null;
                    if (sc.getFatherId() != null && !sc.getFatherId().equals(s.getId())) {
                        otherParentId = sc.getFatherId();
                    } else if (sc.getMotherId() != null && !sc.getMotherId().equals(s.getId())) {
                        otherParentId = sc.getMotherId();
                    }

                    if (otherParentId != null) {
                        final Long otherParentIdFinal = otherParentId;
                        boolean isSpouse = siblingSpouses.stream()
                                .anyMatch(sbSp -> sbSp.getId().equals(otherParentIdFinal));
                        if (!isSpouse) continue;
                    }

                    addNode(nodeMap, visited, sc, "CHILD");
                    connections.add(conn(s.getId(), sc.getId(), "PARENT_CHILD"));

                    if (sc.getFatherId() != null && !sc.getFatherId().equals(s.getId())
                            && visited.contains(sc.getFatherId())) {
                        connections.add(conn(sc.getFatherId(), sc.getId(), "PARENT_CHILD"));
                    }
                    if (sc.getMotherId() != null && !sc.getMotherId().equals(s.getId())
                            && visited.contains(sc.getMotherId())) {
                        connections.add(conn(sc.getMotherId(), sc.getId(), "PARENT_CHILD"));
                    }
                }
            }
        }
    }

    // =========================================================
    // BOLALAR — rekursiv, ixtiyoriy chuqurlik
    // =========================================================
    private void placeChildren(Person center,
            Map<Long, TreeNodeDtoNew> nodeMap,
            List<TreeLayoutResponseDtoNew.ConnectionDto> connections,
            Set<Long> visited) {
        addDescendantsRecursive(center, nodeMap, connections, visited, 0);
    }

    /**
     * Berilgan shaxsning barcha avlodlarini (children, grandchildren, ...)
     * rekursiv ravishda qo'shadi. X/Y koordinatalar null — frontend hisoblaydi.
     */
    private void addDescendantsRecursive(Person parent,
            Map<Long, TreeNodeDtoNew> nodeMap,
            List<TreeLayoutResponseDtoNew.ConnectionDto> connections,
            Set<Long> visited,
            int depth) {

        if (depth > 20) return; // Cheksiz loopdan himoya

        List<Person> children = personRepository.findAllByFatherIdOrMotherId(
                parent.getId(), parent.getId());

        if (children == null || children.isEmpty()) return;

        for (Person child : children) {
            if (visited.contains(child.getId())) continue;

            addNode(nodeMap, visited, child, "CHILD");

            // Ota/ona → farzand connection
            connections.add(conn(parent.getId(), child.getId(), "PARENT_CHILD"));

            // Farzandning ikkinchi ota-onasi bilan connection
            if (child.getFatherId() != null && !child.getFatherId().equals(parent.getId())
                    && visited.contains(child.getFatherId())) {
                connections.add(conn(child.getFatherId(), child.getId(), "PARENT_CHILD"));
            }
            if (child.getMotherId() != null && !child.getMotherId().equals(parent.getId())
                    && visited.contains(child.getMotherId())) {
                connections.add(conn(child.getMotherId(), child.getId(), "PARENT_CHILD"));
            }

            // Farzandning juft(lar)ini qo'shish
            List<Person> childSpouses = relationRepository.findAllSpousesNative(child.getId());
            for (Person csp : childSpouses) {
                if (visited.contains(csp.getId())) {
                    connections.add(conn(child.getId(), csp.getId(), "SPOUSE"));
                    continue;
                }
                addNode(nodeMap, visited, csp, "SPOUSE");
                connections.add(conn(child.getId(), csp.getId(), "SPOUSE"));
            }

            // Rekursiv: farzandning avlodlari
            addDescendantsRecursive(child, nodeMap, connections, visited, depth + 1);
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /**
     * Node qo'shadi. X va Y null — koordinatalar frontendda hisoblanadi.
     */
    private void addNode(Map<Long, TreeNodeDtoNew> nodeMap, Set<Long> visited,
            Person p, String role) {
        nodeMap.put(p.getId(), TreeNodeDtoNew.builder()
                .id(p.getId())
                .name(p.getName())
                .gender(p.getGender() != null ? p.getGender().name() : null)
                .role(role)
                .birthDate(p.getBirthDate())
                .diedDate(p.getDiedDate())
                .photoUrl(p.getPhotoUrl())
                .treeId(p.getFamilyTree() != null ? p.getFamilyTree().getId() : null)
                .fatherId(p.getFatherId())
                .motherId(p.getMotherId())
                .x(null)
                .y(null)
                .build());
        visited.add(p.getId());
    }

    private TreeLayoutResponseDtoNew.ConnectionDto conn(Long from, Long to, String type) {
        return TreeLayoutResponseDtoNew.ConnectionDto.builder()
                .fromId(from).toId(to).type(type).build();
    }
}
