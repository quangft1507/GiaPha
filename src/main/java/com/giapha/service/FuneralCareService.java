package com.giapha.service;

import com.giapha.dto.FuneralCareDTO;
import com.giapha.dto.FuneralCareRequest;
import com.giapha.entity.FuneralCare;
import com.giapha.entity.Person;
import com.giapha.repository.FuneralCareRepository;
import com.giapha.repository.PersonRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class FuneralCareService {
    private final FuneralCareRepository funeralCareRepository;
    private final PersonRepository personRepository;

    public FuneralCareDTO assignCare(Long deceasedPersonId, FuneralCareRequest request) {
        Person deceased = personRepository.findById(deceasedPersonId)
                .orElseThrow(() -> new RuntimeException("Deceased person not found"));
        Person caretaker = personRepository.findById(request.getCaretakerPersonId())
                .orElseThrow(() -> new RuntimeException("Caretaker not found"));

        FuneralCare care = new FuneralCare();
        care.setDeceasedPerson(deceased);
        care.setCaretakerPerson(caretaker);
        care.setCareType(request.getCareType());
        care.setNotes(request.getNotes());
        care.setAssignedDate(request.getAssignedDate());
        care.setIsActive(true);

        return toDTO(funeralCareRepository.save(care));
    }

    public FuneralCareDTO updateCare(Long careId, FuneralCareRequest request) {
        FuneralCare care = funeralCareRepository.findById(careId)
                .orElseThrow(() -> new RuntimeException("Funeral care not found"));
        Person caretaker = personRepository.findById(request.getCaretakerPersonId())
                .orElseThrow(() -> new RuntimeException("Caretaker not found"));

        care.setCaretakerPerson(caretaker);
        care.setCareType(request.getCareType());
        care.setNotes(request.getNotes());
        care.setAssignedDate(request.getAssignedDate());

        return toDTO(funeralCareRepository.save(care));
    }

    public void removeCare(Long careId) {
        funeralCareRepository.deleteById(careId);
    }

    public List<FuneralCareDTO> getCareForPerson(Long personId) {
        return funeralCareRepository.findByDeceasedPersonId(personId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<FuneralCareDTO> getCareByCaretaker(Long personId) {
        return funeralCareRepository.findByCaretakerPersonId(personId)
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    private FuneralCareDTO toDTO(FuneralCare care) {
        return FuneralCareDTO.builder()
                .id(care.getId())
                .deceasedPersonId(care.getDeceasedPerson().getId())
                .deceasedPersonName(care.getDeceasedPerson().getFullName())
                .caretakerPersonId(care.getCaretakerPerson().getId())
                .caretakerPersonName(care.getCaretakerPerson().getFullName())
                .careType(care.getCareType())
                .careTypeDisplayName(care.getCareType().getDisplayName())
                .notes(care.getNotes())
                .assignedDate(care.getAssignedDate())
                .isActive(care.getIsActive())
                .build();
    }
}
