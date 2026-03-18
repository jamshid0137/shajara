package com.example.shajara.service;

import com.example.shajara.dto.layout.TreeLayoutResponseDto;
import com.example.shajara.dto.layout.TreeNodeDto;
import com.example.shajara.entity.Person;
import com.example.shajara.exception.NotFoundException;
import com.example.shajara.repository.PersonRepository;
import com.example.shajara.repository.RelationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Shajara layout algoritmi (V6 — adaptatsiya).
 *
 * Bu loyihada ma'lumotlar tuzilmasi:
 * - Person.fatherId / Person.motherId → ota-ona
 * - Relation (type=SPOUSE) → turmush o'rtoqlari
 * - findAllByFatherIdOrMotherId() → bolalar
 *
 * Layout:
 * Y = -V_SPACE → [ONA] [OTA]
 * Y = 0 → [SP-L] ... [CENTER] ... [SP-R] (vertikal ustunlarda)
 * Y = +V_SPACE → [bola1] [bola2] [bola3] ...
 */
@Service
@RequiredArgsConstructor
public class TreeLayoutService {

    private final PersonRepository personRepository;
    private final RelationRepository relationRepository;

    // ===== KONSTANTALAR =====
    private static final double NODE_W = 200.0;
    private static final double NODE_H = 80.0;
    private static final double H_GAP = 40.0;
    private static final double V_SPACE = 260.0;
    private static final double CHILD_GAP = 30.0;

    private static final double RIGHT_COL_X = NODE_W + H_GAP;
    private static final double LEFT_COL_X = -(NODE_W + H_GAP);

    @Transactional(readOnly = true)
    public TreeLayoutResponseDto calculateLayout(Long personId) {
        Person center = personRepository.findById(personId)
                .orElseThrow(() -> new NotFoundException("Person not found: " + personId));

        Map<Long, TreeNodeDto> nodeMap = new LinkedHashMap<>();
        List<TreeLayoutResponseDto.ConnectionDto> connections = new ArrayList<>();
        Set<Long> visited = new HashSet<>();

        // 1. MARKAZ
        addNode(nodeMap, visited, center, 0, 0, "CENTER");

        // 2. OTA-ONALAR
        placeParents(center, nodeMap, connections, visited);

        // 3. TURMUSH O'RTOQLARI — ikki tomonda vertikal ustun
        placeSpouses(center, nodeMap, connections, visited);

        // 4. AKA-UKALAR
        placeSiblings(center, nodeMap, connections, visited);

        // 5. Spouselar qanchalik pastga tushganini aniqlaymiz
        double maxSpouseBottom = 0;
        for (TreeNodeDto n : nodeMap.values()) {
            if ("SPOUSE".equals(n.getRole())) {
                maxSpouseBottom = Math.max(maxSpouseBottom, n.getY() + NODE_H);
            }
        }
        // Children: spouselar pastigidan yetarli bo'shliq qoldirgan holda
        double childY = Math.max(V_SPACE, maxSpouseBottom + V_SPACE * 0.6);

        // 6. BOLALAR (dinamik Y)
        placeChildren(center, nodeMap, connections, visited, childY);

        // 7. OVERLAP RESOLUTION
        resolveOverlaps(nodeMap);

        // 8. CHEGARALAR
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;
        for (TreeNodeDto n : nodeMap.values()) {
            minX = Math.min(minX, n.getX());
            maxX = Math.max(maxX, n.getX() + NODE_W);
            minY = Math.min(minY, n.getY());
            maxY = Math.max(maxY, n.getY() + NODE_H);
        }

        return TreeLayoutResponseDto.builder()
                .nodes(new ArrayList<>(nodeMap.values()))
                .connections(connections)
                .minX(minX).maxX(maxX)
                .minY(minY).maxY(maxY)
                .build();
    }

    // =========================================================
    // OTA-ONALAR (Y = -V_SPACE)
    // =========================================================
    private void placeParents(Person center,
            Map<Long, TreeNodeDto> nodeMap,
            List<TreeLayoutResponseDto.ConnectionDto> connections,
            Set<Long> visited) {
        double y = -V_SPACE;

        if (center.getFatherId() != null) {
            personRepository.findById(center.getFatherId()).ifPresent(father -> {
                if (!visited.contains(father.getId())) {
                    double fX = NODE_W / 2.0 + H_GAP;
                    addNode(nodeMap, visited, father, fX, y, "FATHER");
                    connections.add(conn(father.getId(), center.getId(), "PARENT_CHILD"));

                    // OTA ning OTA-ONASI (Buvasi va buvisi)
                    if (father.getFatherId() != null) {
                        personRepository.findById(father.getFatherId()).ifPresent(gf -> {
                            if (!visited.contains(gf.getId())) {
                                addNode(nodeMap, visited, gf, fX + NODE_W / 2.0, y - V_SPACE, "FATHER");
                                connections.add(conn(gf.getId(), father.getId(), "PARENT_CHILD"));
                            }
                        });
                    }
                    if (father.getMotherId() != null) {
                        personRepository.findById(father.getMotherId()).ifPresent(gm -> {
                            if (!visited.contains(gm.getId())) {
                                addNode(nodeMap, visited, gm, fX - NODE_W / 2.0, y - V_SPACE, "MOTHER");
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
                    double mX = -(NODE_W / 2.0 + H_GAP + NODE_W);
                    addNode(nodeMap, visited, mother, mX, y, "MOTHER");
                    connections.add(conn(mother.getId(), center.getId(), "PARENT_CHILD"));
                    // Ota va ona o'rtasida SPOUSE liniyasi
                    if (center.getFatherId() != null) {
                        connections.add(conn(center.getFatherId(), mother.getId(), "SPOUSE"));
                    }

                    // ONA ning OTA-ONASI (Buvasi va buvisi)
                    if (mother.getFatherId() != null) {
                        personRepository.findById(mother.getFatherId()).ifPresent(gf -> {
                            if (!visited.contains(gf.getId())) {
                                addNode(nodeMap, visited, gf, mX + NODE_W / 2.0, y - V_SPACE, "FATHER");
                                connections.add(conn(gf.getId(), mother.getId(), "PARENT_CHILD"));
                            }
                        });
                    }
                    if (mother.getMotherId() != null) {
                        personRepository.findById(mother.getMotherId()).ifPresent(gm -> {
                            if (!visited.contains(gm.getId())) {
                                addNode(nodeMap, visited, gm, mX - NODE_W / 2.0, y - V_SPACE, "MOTHER");
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
    // TURMUSH O'RTOQLARI — markaziy shaxs bilan bir qatorda (Y=0)
    // =========================================================
    private void placeSpouses(Person center,
            Map<Long, TreeNodeDto> nodeMap,
            List<TreeLayoutResponseDto.ConnectionDto> connections,
            Set<Long> visited) {
        List<Person> spouses = relationRepository.findAllSpousesNative(center.getId());
        if (spouses == null || spouses.isEmpty()) return;

        int rightIdx = 0;

        for (int i = 0; i < spouses.size(); i++) {
            Person spouse = spouses.get(i);
            if (visited.contains(spouse.getId())) continue;

            double x = RIGHT_COL_X + (rightIdx * (NODE_W + H_GAP));
            double y = 0;
            rightIdx++;

            addNode(nodeMap, visited, spouse, x, y, "SPOUSE");
            connections.add(conn(center.getId(), spouse.getId(), "SPOUSE"));

            // ===== SPOUSE NING OTA-ONASI =====
            if (spouse.getFatherId() != null) {
                personRepository.findById(spouse.getFatherId()).ifPresent(father -> {
                    if (!visited.contains(father.getId())) {
                        addNode(nodeMap, visited, father, x + (NODE_W / 2.0), y - V_SPACE, "FATHER");
                        connections.add(conn(father.getId(), spouse.getId(), "PARENT_CHILD"));
                    }
                });
            }

            if (spouse.getMotherId() != null) {
                personRepository.findById(spouse.getMotherId()).ifPresent(mother -> {
                    if (!visited.contains(mother.getId())) {
                        addNode(nodeMap, visited, mother, x - (NODE_W / 2.0), y - V_SPACE, "MOTHER");
                        connections.add(conn(mother.getId(), spouse.getId(), "PARENT_CHILD"));
                        if (spouse.getFatherId() != null) {
                            connections.add(conn(spouse.getFatherId(), mother.getId(), "SPOUSE"));
                        }
                    }
                });
            }

            // ===== SPOUSE NING O'Z (BOSHQA) FARZANDLARI =====
            List<Person> spouseChildren = personRepository.findAllByFatherIdOrMotherId(spouse.getId(), spouse.getId());
            int childCount = 0;
            for (Person spChild : spouseChildren) {
                if (!visited.contains(spChild.getId())) {
                    
                    // Bolaning 2-ota-onasini aniqlaymiz
                    Long otherParentId = null;
                    if (spChild.getFatherId() != null && !spChild.getFatherId().equals(spouse.getId())) {
                        otherParentId = spChild.getFatherId();
                    } else if (spChild.getMotherId() != null && !spChild.getMotherId().equals(spouse.getId())) {
                        otherParentId = spChild.getMotherId();
                    }

                    // Agar bola boshqa spouse bilan bo'lsa (ya'ni 2-ota-onasi markaziy Person bo'lmasa),
                    // bu bolani shu markaziy kishi ko'rinishida yashiramiz (continue).
                    if (otherParentId != null && !otherParentId.equals(center.getId())) {
                        continue;
                    }

                    // Spousening tagiga ustma-ust (vertikal) joylash
                    double spChildX = x; 
                    double spChildY = y + V_SPACE + (childCount * (NODE_H + CHILD_GAP));
                    
                    addNode(nodeMap, visited, spChild, spChildX, spChildY, "CHILD");
                    connections.add(conn(spouse.getId(), spChild.getId(), "PARENT_CHILD"));
                    childCount++;

                    if (spChild.getFatherId() != null && !spChild.getFatherId().equals(spouse.getId())) {
                        Long otherId = spChild.getFatherId();
                        // Faqat already visited bo'lsa connection qo'shamiz
                        if (visited.contains(otherId)) {
                            connections.add(conn(otherId, spChild.getId(), "PARENT_CHILD"));
                        }
                    }
                    if (spChild.getMotherId() != null && !spChild.getMotherId().equals(spouse.getId())) {
                        Long otherId = spChild.getMotherId();
                        // Faqat already visited bo'lsa connection qo'shamiz
                        if (visited.contains(otherId)) {
                            connections.add(conn(otherId, spChild.getId(), "PARENT_CHILD"));
                        }
                    }
                }
            }
        }
    }

    // =========================================================
    // AKA-UKALAR — markaziy shaxs bilan bir qatorda (chaproqda, Y=0)
    // =========================================================
    private void placeSiblings(Person center,
            Map<Long, TreeNodeDto> nodeMap,
            List<TreeLayoutResponseDto.ConnectionDto> connections,
            Set<Long> visited) {
        if (center.getFatherId() == null && center.getMotherId() == null) return;

        Long fId = center.getFatherId() != null ? center.getFatherId() : -1L;
        Long mId = center.getMotherId() != null ? center.getMotherId() : -1L;
        List<Person> siblings = personRepository.findAllByFatherIdOrMotherId(fId, mId);

        double sibXBase = LEFT_COL_X;

        for (Person s : siblings) {
            if (s.getId().equals(center.getId()) || visited.contains(s.getId())) continue;

            List<Person> siblingSpouses = relationRepository.findAllSpousesNative(s.getId());
            
            // SibX calculation: we move left by enough space for sibling + their spouses
            int slotCount = 1 + siblingSpouses.size();
            sibXBase -= (slotCount * (NODE_W + H_GAP));
            
            double currX = sibXBase;

            addNode(nodeMap, visited, s, currX, 0, "SIBLING");

            if (center.getFatherId() != null) {
                connections.add(conn(center.getFatherId(), s.getId(), "PARENT_CHILD"));
            } else if (center.getMotherId() != null) {
                connections.add(conn(center.getMotherId(), s.getId(), "PARENT_CHILD"));
            }

            // AKA-UKANING SPOUSE LARI
            for (int k = 0; k < siblingSpouses.size(); k++) {
                Person sbSp = siblingSpouses.get(k);
                if (!visited.contains(sbSp.getId())) {
                    currX += NODE_W + H_GAP;
                    addNode(nodeMap, visited, sbSp, currX, 0, "SPOUSE");
                    connections.add(conn(s.getId(), sbSp.getId(), "SPOUSE"));
                }
            }

            // AKA-UKANING NEVARALARI (Children of sibling)
            List<Person> siblingChildren = personRepository.findAllByFatherIdOrMotherId(s.getId(), s.getId());
            Map<Long, Integer> childCountsByParent = new HashMap<>();

            for (Person sc : siblingChildren) {
                if (!visited.contains(sc.getId())) {
                    Long otherParentId = null;
                    if (sc.getFatherId() != null && !sc.getFatherId().equals(s.getId())) {
                        otherParentId = sc.getFatherId();
                    } else if (sc.getMotherId() != null && !sc.getMotherId().equals(s.getId())) {
                        otherParentId = sc.getMotherId();
                    }

                    Long placementTargetId = s.getId();

                    if (otherParentId != null) {
                        boolean isSpouse = false;
                        for (Person sbSp : siblingSpouses) {
                            if (sbSp.getId().equals(otherParentId)) {
                                isSpouse = true;
                                break;
                            }
                        }
                        if (isSpouse) {
                            placementTargetId = otherParentId;
                        } else {
                            continue; // Boshqa noma'lum spouse bo'lsa, yashiramiz
                        }
                    }

                    int currentCount = childCountsByParent.getOrDefault(placementTargetId, 0);
                    TreeNodeDto targetNode = nodeMap.get(placementTargetId);
                    double cx = targetNode != null ? targetNode.getX() : sibXBase;
                    double cy = V_SPACE + (currentCount * (NODE_H + CHILD_GAP));

                    addNode(nodeMap, visited, sc, cx, cy, "CHILD");
                    connections.add(conn(s.getId(), sc.getId(), "PARENT_CHILD"));
                    childCountsByParent.put(placementTargetId, currentCount + 1);

                    if (sc.getFatherId() != null && !sc.getFatherId().equals(s.getId()) && visited.contains(sc.getFatherId())) {
                        connections.add(conn(sc.getFatherId(), sc.getId(), "PARENT_CHILD"));
                    }
                    if (sc.getMotherId() != null && !sc.getMotherId().equals(s.getId()) && visited.contains(sc.getMotherId())) {
                        connections.add(conn(sc.getMotherId(), sc.getId(), "PARENT_CHILD"));
                    }
                }
            }
        }
    }

    // =========================================================
    // BOLALAR — rekursiv, ixtiyoriy chuqurlik
    // FamilyTree.js koordinatani o'zi hisoblaydi,
    // biz faqat nodelar va connectionlarni beramiz.
    // =========================================================
    private void placeChildren(Person center,
            Map<Long, TreeNodeDto> nodeMap,
            List<TreeLayoutResponseDto.ConnectionDto> connections,
            Set<Long> visited,
            double childY) {
        // Rekursiv traversal — childY muhim emas (FamilyTree.js layout qiladi)
        addDescendantsRecursive(center, nodeMap, connections, visited, childY, 0);
    }

    /**
     * Berilgan shaxsning barcha avlodlarini (children, grandchildren, ...)
     * rekursiv ravishda qo'shadi. Har bir avlodning:
     * - O'zi
     * - Juft(lar)i (SPOUSE connection bilan)
     * - Farzandlari (rekursiv)
     */
    private void addDescendantsRecursive(Person parent,
            Map<Long, TreeNodeDto> nodeMap,
            List<TreeLayoutResponseDto.ConnectionDto> connections,
            Set<Long> visited,
            double baseY,
            int depth) {

        if (depth > 20)
            return; // Cheksiz loopdan himoya (amalda 20 avlod yetarli)

        List<Person> children = personRepository.findAllByFatherIdOrMotherId(
                parent.getId(), parent.getId());

        if (children == null || children.isEmpty())
            return;

        TreeNodeDto parentNode = nodeMap.get(parent.getId());
        double parentX = parentNode != null ? parentNode.getX() : 0.0;

        int childCount = 0;
        for (int i = 0; i < children.size(); i++) {
            Person child = children.get(i);
            if (visited.contains(child.getId()))
                continue;

            // O'zi bilan ustma-ust tushishi uchun X o'zgarmas qoladi, Y esa ortadi
            double cx = parentX;
            double cy = baseY + childCount * (NODE_H + CHILD_GAP);
            addNode(nodeMap, visited, child, cx, cy, "CHILD");
            childCount++;

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

            // ── Farzandning juft(lar)ini qo'shish ──
            List<Person> childSpouses = relationRepository.findAllSpousesNative(child.getId());
            for (int j = 0; j < childSpouses.size(); j++) {
                Person csp = childSpouses.get(j);
                if (visited.contains(csp.getId())) {
                    // Allaqachon qo'shilgan → faqat connection
                    connections.add(conn(child.getId(), csp.getId(), "SPOUSE"));
                    continue;
                }
                // Juft yoniga joylaymiz
                double cspX = cx + (j % 2 == 0 ? NODE_W + H_GAP : -(NODE_W + H_GAP));
                addNode(nodeMap, visited, csp, cspX, cy, "SPOUSE");
                connections.add(conn(child.getId(), csp.getId(), "SPOUSE"));
            }

            // ── Rekursiv: farzandning avlodlari ──
            addDescendantsRecursive(child, nodeMap, connections, visited,
                    cy + V_SPACE, depth + 1);
        }
    }

    // =========================================================
    // OVERLAP RESOLUTION
    // Bir xil Y qatlamdagi nodelar X bo'yicha sortlanib,
    // ustma-ust tushsa o'ngga suriladi.
    // =========================================================
    private void resolveOverlaps(Map<Long, TreeNodeDto> nodeMap) {
        Map<Double, List<TreeNodeDto>> byRow = new LinkedHashMap<>();
        for (TreeNodeDto n : nodeMap.values()) {
            byRow.computeIfAbsent(n.getY(), k -> new ArrayList<>()).add(n);
        }
        for (List<TreeNodeDto> row : byRow.values()) {
            row.sort(Comparator.comparingDouble(TreeNodeDto::getX));
            for (int i = 1; i < row.size(); i++) {
                TreeNodeDto prev = row.get(i - 1);
                TreeNodeDto curr = row.get(i);
                double minX = prev.getX() + NODE_W + H_GAP;
                if (curr.getX() < minX) {
                    curr.setX(minX);
                }
            }
        }
    }

    // =========================================================
    // HELPERS
    // =========================================================
    private void addNode(Map<Long, TreeNodeDto> nodeMap, Set<Long> visited,
            Person p, double x, double y, String role) {
        nodeMap.put(p.getId(), TreeNodeDto.builder()
                .id(p.getId())
                .name(p.getName())
                .gender(p.getGender() != null ? p.getGender().name() : null)
                .role(role)
                .birthDate(p.getBirthDate())
                .diedDate(p.getDiedDate())
                .photoUrl(p.getPhotoUrl())
                .treeId(p.getFamilyTree() != null ? p.getFamilyTree().getId() : null)
                .fatherId(p.getFatherId()) // ✅ to'g'ridan DB dan
                .motherId(p.getMotherId()) // ✅ to'g'ridan DB dan
                .x(x)
                .y(y)
                .build());
        visited.add(p.getId());
    }

    private TreeLayoutResponseDto.ConnectionDto conn(Long from, Long to, String type) {
        return TreeLayoutResponseDto.ConnectionDto.builder()
                .fromId(from).toId(to).type(type).build();
    }
}
