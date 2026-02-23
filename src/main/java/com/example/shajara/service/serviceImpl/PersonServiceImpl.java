package com.example.shajara.service.serviceImpl;

//import com.example.shajara.config.S3SignedUrlService;
import com.example.shajara.dto.person.PersonAddParentDto;
import com.example.shajara.dto.person.*;
import com.example.shajara.entity.FamilyTree;
import com.example.shajara.entity.Person;
import com.example.shajara.entity.Relation;
import com.example.shajara.enums.Gender;
import com.example.shajara.enums.RelationType;
import com.example.shajara.exception.AppBadException;
import com.example.shajara.exception.NotFoundException;
import com.example.shajara.repository.FamilyTreeRepository;
import com.example.shajara.repository.PersonRepository;
import com.example.shajara.repository.RelationRepository;
import com.example.shajara.service.PersonService;
import com.example.shajara.service.RelationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class PersonServiceImpl implements PersonService {

    private final PersonRepository personRepository;
    private final FamilyTreeRepository familyTreeRepository;
    private final RelationService relationService;
    private final RelationRepository relationRepository;

    private final S3Client s3Client; // todo

    // private final S3SignedUrlService s3SignedUrlService;
    //
    // public String getPersonPhotoSignedUrl(Person person) {
    // if(person.getPhotoUrl() == null) return null;
    //
    // String bucketName = "shajara-person-photos";
    // // URLdan keyni olish
    // String key =
    // person.getPhotoUrl().substring(person.getPhotoUrl().lastIndexOf("/") + 1);
    //
    // // 60 daqiqa amal qiladigan URL
    // return s3SignedUrlService.generateSignedUrl(bucketName, key, 60);
    // }

    @Override
    public PersonAddChildResponseDto addChild(PersonAddChildDto dto) {
        Person parent = personRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("person not found"));
        FamilyTree familyTree = familyTreeRepository.findById(dto.getTreeId())
                .orElseThrow(() -> new NotFoundException("family not found"));
        Person child = new Person();
        child.setFamilyTree(familyTree);
        child.setGender(dto.getChildGender());
        child.setName("child");
        // ✅ Parent erkak bo'lsa fatherId, ayol bo'lsa motherId
        if (parent.getGender() == Gender.MALE) {
            child.setFatherId(parent.getId());
        } else {
            child.setMotherId(parent.getId());
        }
        child = personRepository.save(child);
        return new PersonAddChildResponseDto(parent.getId(), child.getId(), dto.getTreeId());
    }

    @Override
    @Transactional
    public PersonAddSpouseDto addSpouse(PersonAddSpouseCreateDto dto) {

        Person eri = personRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("person not found"));
        Person spouse = new Person();
        FamilyTree familyTree = familyTreeRepository.findById(dto.getTreeId())
                .orElseThrow(() -> new NotFoundException("family not found"));

        spouse.setFamilyTree(familyTree);
        if (eri.getGender() == Gender.FEMALE)
            spouse.setGender(Gender.MALE);
        if (eri.getGender() == Gender.MALE)
            spouse.setGender(Gender.FEMALE);
        spouse.setName("spouse");
        spouse = personRepository.save(spouse);

        Relation relation = Relation.builder()
                .fromPerson(eri)
                .toPerson(spouse)
                .type(RelationType.SPOUSE)
                .build();
        relationRepository.save(relation); // ✅ DB ga saqlanadi
        return new PersonAddSpouseDto(eri.getId(), spouse.getId(), dto.getTreeId());
    }

    @Override
    @Transactional
    public PersonAddParentDto addParents(PersonAddParentDto dto) {
        Person child = personRepository.findById(dto.getId())
                .orElseThrow(() -> new NotFoundException("person not found"));
        if (child.getFatherId() != null && child.getFatherId() != 0
                && child.getMotherId() != null && child.getMotherId() != 0) {
            throw new AppBadException("Allaqachon otasiham  onasiham bor !");
        }
        FamilyTree familyTree = familyTreeRepository.findById(dto.getTreeId())
                .orElseThrow(() -> new NotFoundException("family not found"));

        // FATHER
        if (child.getFatherId() == null || child.getFatherId() == 0) {
            if (dto.getFatherId() != null && dto.getFatherId() != 0) {
                // Berilgan fatherId bo'yicha mavjud personni bog'laymiz
                Person father = personRepository.findById(dto.getFatherId())
                        .orElseThrow(() -> new NotFoundException("Father person not found: " + dto.getFatherId()));
                child.setFatherId(father.getId());
            } else {
                // Yangi father yaratamiz
                Person father = new Person();
                father.setFamilyTree(familyTree);
                father.setGender(Gender.MALE);
                father.setName("Father");
                father = personRepository.save(father);
                child.setFatherId(father.getId());
            }
        }

        // MOTHER
        if (child.getMotherId() == null || child.getMotherId() == 0) {
            if (dto.getMotherId() != null && dto.getMotherId() != 0) {
                // Berilgan motherId bo'yicha mavjud personni bog'laymiz
                Person mother = personRepository.findById(dto.getMotherId())
                        .orElseThrow(() -> new NotFoundException("Mother person not found: " + dto.getMotherId()));
                child.setMotherId(mother.getId());
            } else {
                // Yangi mother yaratamiz
                Person mother = new Person();
                mother.setFamilyTree(familyTree);
                mother.setGender(Gender.FEMALE);
                mother.setName("Mother");
                mother = personRepository.save(mother);
                child.setMotherId(mother.getId());
            }
        }

        personRepository.save(child);
        return new PersonAddParentDto(dto.getId(), child.getFatherId(), child.getMotherId(), dto.getTreeId());
    }

    @Override
    public PersonResponseDto create(PersonCreateDto dto) {

        FamilyTree tree = familyTreeRepository.findById(dto.getTreeId())
                .orElseThrow(() -> new NotFoundException("Family tree not found"));

        Person person = Person.builder()
                .name(dto.getName())
                .gender(dto.getGender() != null ? dto.getGender() : Gender.MALE)
                .birthDate(dto.getBirthDate())
                .profession(dto.getProfession())
                .homeland(dto.getHomeland())
                .diedDate(dto.getDiedDate())
                .phoneNumber(dto.getPhoneNumber())
                // TEKSHIRISH KERAKMI BULARNI-MAVJUD VA MAVJUDMASLIKKA MASALAN
                .motherId(dto.getMotherId())
                .fatherId(dto.getFatherId())
                .familyTree(tree)
                .build();

        person = personRepository.save(person);

        // ===== S3 ga rasm yuklash =====
        // ===== Gender bo'yicha default rasm qo'yish =====
        String bucketName = "shajara-person-photos";
        if (person.getGender() == Gender.MALE) {
            person.setPhotoUrl("https://" + bucketName + ".s3.us-east-1.amazonaws.com/default-male.png");
        } else {
            person.setPhotoUrl("https://" + bucketName + ".s3.us-east-1.amazonaws.com/default-female.jpg");
        }

        personRepository.save(person);

        return toDto(person);
    }

    @Override
    public List<PersonResponseDto> getAllByTree(Long treeId) {
        return personRepository.findAllByFamilyTreeId(treeId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    @Override
    public PersonResponseDto getById(Long id) {
        return toDto(find(id));
    }

    @Override
    @Transactional
    public PersonResponseDto update(Long id, PersonUpdateDto dto) {
        Person person = find(id);

        if (dto.getName() != null)
            person.setName(dto.getName());
        if (dto.getGender() != null)
            person.setGender(dto.getGender());
        if (dto.getBirthDate() != null)
            person.setBirthDate(dto.getBirthDate());
        if (dto.getProfession() != null)
            person.setProfession(dto.getProfession());
        if (dto.getHomeland() != null)
            person.setHomeland(dto.getHomeland());
        person.setDiedDate(dto.getDiedDate());
        if (dto.getPhoneNumber() != null)
            person.setPhoneNumber(dto.getPhoneNumber());

        // Father va Mother update
        // bitta treeda bo'lishini va unaqa odam bor yo'qligini tekshirish kerakov !
        // add parentdagi metodlar yordamida
        // null qiymat uchun ham yozish kerak yani bu relation emas endi shunchaki null
        // qilsak yetarli bo'ladi

        if (dto.getFatherId() == null)
            person.setFatherId(dto.getFatherId());
        if (dto.getMotherId() == null)
            person.setMotherId(dto.getMotherId());

        if (dto.getFatherId() != null && dto.getFatherId() != 0) {
            Person newFather = personRepository.findById(dto.getFatherId())
                    .orElseThrow(() -> new NotFoundException("new father id not found"));

            if (!person.getFamilyTree().getId().equals(newFather.getFamilyTree().getId())) {
                throw new AppBadException("These people don't belong to one family tree !");
            }

            boolean qarindoshmasmi = qarindoshmasmi(person, newFather);
            if (qarindoshmasmi) {
                throw new AppBadException("These people are relative !");
            }
            if (newFather.getGender() == Gender.FEMALE) {
                throw new AppBadException("Father must be MALE !");
            }
            person.setFatherId(dto.getFatherId());
        }

        if (dto.getMotherId() != null && dto.getMotherId() != 0) {
            Person newMother = personRepository.findById(dto.getMotherId())
                    .orElseThrow(() -> new NotFoundException("New mother id not found"));
            ;

            if (!person.getFamilyTree().getId().equals(newMother.getFamilyTree().getId())) {
                throw new AppBadException("These people don't belong to one family tree !");
            }

            boolean qarindoshmasmi = qarindoshmasmi(person, newMother);
            if (qarindoshmasmi) {
                throw new AppBadException("These people are relative !");
            }
            if (newMother.getGender() == Gender.MALE) {
                throw new AppBadException("Mother must be FEMALE !");
            }
            person.setMotherId(dto.getMotherId());
        }

        return toDto(personRepository.save(person));
    }

    @Override
    public void delete(Long id) {

        // rasmi uchun bazadan o'chirib yuborish
        Person person = find(id);

        String bucketName = "shajara-person-photos";

        // 1️⃣ Oldingi rasmni o'chirish (agar default emas bo'lsa)
        if (person.getPhotoUrl() != null &&
                !person.getPhotoUrl().contains("default-male") &&
                !person.getPhotoUrl().contains("default-female")) {

            String oldKey = person.getPhotoUrl().substring(person.getPhotoUrl().lastIndexOf("/") + 1);

            try {
                s3Client.deleteObject(builder -> builder.bucket(bucketName).key(oldKey));
            } catch (Exception e) {
                e.printStackTrace();
                // Agar delete muvaffaqiyatsiz bo'lsa ham davom etamiz
            }
        }

        //

        // birinchi manashu inson bilan bog'liq relationlarni o'chirib chiqishimiza
        // kerakda !
        List<Long> relations = relationService.findForAllSpousesNativeRelationdIds(id);
        for (Long i : relations) {
            relationService.delete(i);
        }
        // childrenlarini topib fatherid yoki motherid null qilish kerak
        List<Person> children = personRepository.findAllByFatherIdOrMotherId(id, id);
        for (Person p : children) {
            if (p.getFatherId().equals(id))
                p.setFatherId(null);
            if (p.getMotherId().equals(id))
                p.setMotherId(null);
        }
        personRepository.saveAll(children);
        personRepository.delete(find(id));
    }

    public Person find(Long id) {
        return personRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Person not found"));
    }

    private PersonResponseDto toDto(Person p) {
        return PersonResponseDto.builder()
                .id(p.getId())
                .name(p.getName())
                .gender(p.getGender())
                .birthDate(p.getBirthDate())
                .profession(p.getProfession())
                .homeland(p.getHomeland())
                .diedDate(p.getDiedDate())
                .phoneNumber(p.getPhoneNumber())
                .fatherId(p.getFatherId())
                .motherId(p.getMotherId())
                .treeId(p.getFamilyTree().getId())
                .photoUrl(p.getPhotoUrl()) // todo
                .build();
    }

    @Override
    @Transactional
    public PersonResponseDto addParent(Long childId, Long parentId) {
        // 1️⃣ Child va Parentni olish
        Person child = personRepository.findById(childId)
                .orElseThrow(() -> new NotFoundException("Child not found"));

        Person parent = personRepository.findById(parentId)
                .orElseThrow(() -> new NotFoundException("Parent not found"));

        if (!child.getFamilyTree().getId().equals(parent.getFamilyTree().getId())) {
            throw new AppBadException("These people don't belong to one family tree !");
        }

        boolean qarindoshmasmi = qarindoshmasmi(child, parent);
        if (qarindoshmasmi) {
            throw new AppBadException("These people are relative !");
        }

        // 2️⃣ Parentning genderiga qarab fatherId yoki motherId belgilash
        if (parent.getGender() == Gender.MALE) {
            // Agar childda oldin father mavjud bo‘lsa xatolik
            if (child.getFatherId() != null && child.getFatherId() != 0) {
                throw new AppBadException("Child already has a father!");
            }
            child.setFatherId(parent.getId());
        } else if (parent.getGender() == Gender.FEMALE) {
            // Agar childda oldin mother mavjud bo‘lsa xatolik
            if (child.getMotherId() != null && child.getMotherId() != 0) {
                throw new AppBadException("Child already has a mother!");
            }
            child.setMotherId(parent.getId());
        } else {
            throw new AppBadException("Parent gender is undefined!");
        }

        // 3️⃣ Childni saqlash
        personRepository.save(child);

        // 4️⃣ DTO qaytarish
        return toDto(child);
    }

    public boolean qarindoshmasmi(Person from, Person to) {
        // 1️⃣ Spouse tekshiruvlari
        List<Person> fromSpouses = relationRepository.findAllSpousesNative(from.getId());
        for (Person p : fromSpouses) {
            if (p.getId().equals(to.getId()))
                return true;
        }

        // 2️⃣ Parent-Child tekshiruvlari
        if ((from.getFatherId() != null && from.getFatherId().equals(to.getId())) ||
                (from.getMotherId() != null && from.getMotherId().equals(to.getId()))) {
            return true;
        }

        if ((to.getFatherId() != null && to.getFatherId().equals(from.getId())) ||
                (to.getMotherId() != null && to.getMotherId().equals(from.getId()))) {
            return true;
        }

        return false;
    }

    @Override
    @Transactional
    // Person bilan barcha relationlarini olish
    public PersonResponseFullDto getPersonWithRelations(Long personId) {
        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new NotFoundException("Person not found"));

        // Full DTO yaratish
        PersonResponseFullDto dto = mapToFullDto(person);

        // === Parents ===
        if (person.getFatherId() != null) {
            Person father = personRepository.findById(person.getFatherId())
                    .orElse(null); // agar parent o'chirilgan bo'lsa null qaytadi
            if (father != null) {
                dto.setFather(toDto(father));
            }
        }

        if (person.getMotherId() != null) {
            Person mother = personRepository.findById(person.getMotherId())
                    .orElse(null);
            if (mother != null) {
                dto.setMother(toDto(mother));
            }
        }

        // === Spouses ===
        List<Person> spouseRelations = relationRepository.findAllSpousesNative(personId);
        for (Person r : spouseRelations) {
            dto.getSpouses().add(mapToDto(r));
        }

        // === Children ===
        List<Person> children = personRepository.findAllByFatherIdOrMotherId(person.getId(), person.getId());
        for (Person child : children) {
            dto.getChildren().add(mapToDto(child));
        }

        // MANASHU JOYIDA BIZ TREENI LATEST PERSONINI UPDATE QILAMIZA
        person.getFamilyTree().setLastPersonId(person.getId());

        // familyTreeRepository.save(tree);

        return dto;
    }

    // PersonResponseFullDto uchun mapper
    private PersonResponseFullDto mapToFullDto(Person p) {
        PersonResponseFullDto dto = new PersonResponseFullDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setGender(p.getGender());
        dto.setBirthDate(p.getBirthDate());
        dto.setProfession(p.getProfession());
        dto.setHomeland(p.getHomeland());
        dto.setDiedDate(p.getDiedDate());
        dto.setPhoneNumber(p.getPhoneNumber());
        dto.setFatherId(p.getFatherId());
        dto.setMotherId(p.getMotherId());
        dto.setTreeId(p.getFamilyTree().getId());
        dto.setPhotoUrl(p.getPhotoUrl());

        return dto;
    }

    // PersonResponseDto uchun mapper (nested listlarda ishlatiladi)
    private PersonResponseDto mapToDto(Person p) {
        PersonResponseDto dto = new PersonResponseDto();
        dto.setId(p.getId());
        dto.setName(p.getName());
        dto.setGender(p.getGender());
        dto.setBirthDate(p.getBirthDate());
        dto.setProfession(p.getProfession());
        dto.setHomeland(p.getHomeland());
        dto.setDiedDate(p.getDiedDate());
        dto.setPhoneNumber(p.getPhoneNumber());

        dto.setTreeId(p.getFamilyTree().getId());
        dto.setFatherId(p.getFatherId());
        dto.setMotherId(p.getMotherId());
        dto.setPhotoUrl(p.getPhotoUrl());

        return dto;
    }

    public List<PersonSimpleDto> getTreePersons(Long treeId) {
        List<Person> persons = personRepository.findByFamilyTreeId(treeId); // treeId bo'yicha personlarni olish

        return persons.stream()
                .map(p -> PersonSimpleDto.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .build())
                .toList();
    }

    // new todo
    @Override
    @Transactional
    public PersonResponseDto updatePhoto(Long personId, MultipartFile photo) {
        if (photo == null || photo.isEmpty()) {
            throw new AppBadException("Photo file is empty");
        }

        Person person = personRepository.findById(personId)
                .orElseThrow(() -> new NotFoundException("Person not found"));

        String bucketName = "shajara-person-photos";

        // Oldingi rasmni tekshirish va o'chirish (agar default emas bo'lsa)
        if (person.getPhotoUrl() != null &&
                !person.getPhotoUrl().contains("default-male") &&
                !person.getPhotoUrl().contains("default-female")) {

            // Oldingi fayl nomini URL'dan olish
            // https://shajara-person-photos.s3.us-east-1.amazonaws.com/person_12.jpg bo'lsa
            // person_12.jpg ni qirqib olish

            String oldKey = person.getPhotoUrl().substring(person.getPhotoUrl().lastIndexOf("/") + 1);

            try {
                s3Client.deleteObject(builder -> builder.bucket(bucketName).key(oldKey));
            } catch (Exception e) {
                e.printStackTrace();
                // Agar delete muvaffaqiyatsiz bo'lsa ham davom etamiz
            }
        }

        // Yangi rasmni S3 ga yuklash
        String fileExtension = photo.getOriginalFilename().substring(photo.getOriginalFilename().lastIndexOf("."));
        String timestamp = UUID.randomUUID().toString();
        String newFileName = "person_" + person.getId() + timestamp + fileExtension;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(newFileName)
                .contentType(photo.getContentType())
                .build();

        try {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(photo.getInputStream(), photo.getSize()));
        } catch (Exception e) {
            e.printStackTrace();
            throw new AppBadException("Failed to upload photo to S3");
        }

        // Bazaga yangi URL saqlash
        String fileUrl = "https://" + bucketName + ".s3.us-east-1.amazonaws.com/" + newFileName;
        person.setPhotoUrl(fileUrl);
        personRepository.save(person);

        return toDto(person);
    }

}
