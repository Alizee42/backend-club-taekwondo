package club.taekwondo.controller.jpa;

import club.taekwondo.dto.RequiredDocumentDTO;
import club.taekwondo.service.jpa.RequiredDocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/documents-requis")
public class RequiredDocumentController {

    @Autowired
    private RequiredDocumentService service;

    @GetMapping("/club/{clubId}")
    public ResponseEntity<?> getByClub(@PathVariable Long clubId) {
        try {
            List<RequiredDocumentDTO> list = service.getByClub(clubId);
            if (list.isEmpty()) return ResponseEntity.noContent().build();
            return ResponseEntity.ok(list);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody RequiredDocumentDTO dto) {
        try {
            RequiredDocumentDTO saved = service.create(dto);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody RequiredDocumentDTO dto) {
        try {
            RequiredDocumentDTO saved = service.update(id, dto);
            return ResponseEntity.ok(saved);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        try {
            service.delete(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
