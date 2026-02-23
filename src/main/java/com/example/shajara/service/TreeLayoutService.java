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
    private static final double PARTNER_V_GAP = 20.0;
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
                    addNode(nodeMap, visited, father, NODE_W / 2.0 + H_GAP, y, "FATHER");
                    connections.add(conn(father.getId(), center.getId(), "PARENT_CHILD"));
                }
            });
        }

        if (center.getMotherId() != null) {
            personRepository.findById(center.getMotherId()).ifPresent(mother -> {
                if (!visited.contains(mother.getId())) {
                    addNode(nodeMap, visited, mother, -(NODE_W / 2.0 + H_GAP + NODE_W), y, "MOTHER");
                    connections.add(conn(mother.getId(), center.getId(), "PARENT_CHILD"));
                    // Ota va ona o'rtasida SPOUSE liniyasi
                    if (center.getFatherId() != null) {
                        connections.add(conn(center.getFatherId(), mother.getId(), "SPOUSE"));
                    }
                }
            });
        }
    }

    // =========================================================
    // TURMUSH O'RTOQLARI — ikki tomonda vertikal ustun
    // 0-chi: o'ng, 1-chi: chap, 2-chi: o'ng ...
    // =========================================================
    private void placeSpouses(Person center,
            Map<Long, TreeNodeDto> nodeMap,
            List<TreeLayoutResponseDto.ConnectionDto> connections,
            Set<Long> visited) {
        List<Person> spouses = relationRepository.findAllSpousesNative(center.getId());
        if (spouses == null || spouses.isEmpty())
            return;

        double step = NODE_H + PARTNER_V_GAP;
        int rightIdx = 0;
        int leftIdx = 0;

        for (int i = 0; i < spouses.size(); i++) {
            Person spouse = spouses.get(i);
            if (visited.contains(spouse.getId()))
                continue;

            double x, y;
            if (i % 2 == 0) {
                x = RIGHT_COL_X;
                y = rightIdx * step;
                rightIdx++;
            } else {
                x = LEFT_COL_X - NODE_W;
                y = leftIdx * step;
                leftIdx++;
            }

            addNode(nodeMap, visited, spouse, x, y, "SPOUSE");
            connections.add(conn(center.getId(), spouse.getId(), "SPOUSE"));
        }
    }

    // =========================================================
    // AKA-UKALAR — bir xil ota yoki onadan
    // (Center dan chapda, alohida ustunda)
    // =========================================================
    private void placeSiblings(Person center,
            Map<Long, TreeNodeDto> nodeMap,
            List<TreeLayoutResponseDto.ConnectionDto> connections,
            Set<Long> visited) {
        if (center.getFatherId() == null && center.getMotherId() == null)
            return;

        // Bir xil ota yoki onadan bo'lgan odamlarni topamiz
        Long fId = center.getFatherId() != null ? center.getFatherId() : -1L;
        Long mId = center.getMotherId() != null ? center.getMotherId() : -1L;
        List<Person> siblings = personRepository.findAllByFatherIdOrMotherId(fId, mId);

        double step = NODE_H + PARTNER_V_GAP;
        double sibX = LEFT_COL_X - 2 * NODE_W - H_GAP;
        int count = 0;

        for (Person s : siblings) {
            if (s.getId().equals(center.getId()) || visited.contains(s.getId()))
                continue;

            addNode(nodeMap, visited, s, sibX, count * step, "SIBLING");
            count++;

            if (center.getFatherId() != null) {
                connections.add(conn(center.getFatherId(), s.getId(), "PARENT_CHILD"));
            } else if (center.getMotherId() != null) {
                connections.add(conn(center.getMotherId(), s.getId(), "PARENT_CHILD"));
            }
        }
    }

    // =========================================================
    // BOLALAR — dinamik Y (spouselar tagidan)
    // =========================================================
    private void placeChildren(Person center,
            Map<Long, TreeNodeDto> nodeMap,
            List<TreeLayoutResponseDto.ConnectionDto> connections,
            Set<Long> visited,
            double childY) {
        List<Person> children = personRepository.findAllByFatherIdOrMotherId(
                center.getId(), center.getId());
        if (children == null || children.isEmpty())
            return;

        int n = children.size();
        double totalW = n * NODE_W + (n - 1) * CHILD_GAP;
        double startX = -totalW / 2.0;

        for (int i = 0; i < n; i++) {
            Person child = children.get(i);
            if (visited.contains(child.getId()))
                continue;

            double cx = startX + i * (NODE_W + CHILD_GAP);
            addNode(nodeMap, visited, child, cx, childY, "CHILD");

            connections.add(conn(center.getId(), child.getId(), "PARENT_CHILD"));

            if (child.getFatherId() != null && !child.getFatherId().equals(center.getId())
                    && nodeMap.containsKey(child.getFatherId())) {
                connections.add(conn(child.getFatherId(), child.getId(), "PARENT_CHILD"));
            }
            if (child.getMotherId() != null && !child.getMotherId().equals(center.getId())
                    && nodeMap.containsKey(child.getMotherId())) {
                connections.add(conn(child.getMotherId(), child.getId(), "PARENT_CHILD"));
            }
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
