package com.library.member;

import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/members")
public class MemberController {
    private final MemberRepository members;

    public MemberController(MemberRepository members) {
        this.members = members;
    }

    @GetMapping
    public List<Member> list(@RequestParam(required = false) String q) {
        if (StringUtils.hasText(q)) {
            return members.search(Pattern.quote(q.trim()));
        }
        return members.findAll(Sort.by("lastName", "firstName"));
    }

    @GetMapping("/{id}")
    public Member get(@PathVariable String id) {
        return find(id);
    }

    /** Batch lookup used by other services (e.g. notification-service). */
    @GetMapping("/batch")
    public Iterable<Member> batch(@RequestParam List<String> ids) {
        return members.findAllById(ids);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Member create(@Valid @RequestBody MemberRequest req) {
        if (members.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu e-posta ile kayıtlı bir üye var");
        }
        Member m = new Member();
        req.applyTo(m);
        return members.save(m);
    }

    @PutMapping("/{id}")
    public Member update(@PathVariable String id, @Valid @RequestBody MemberRequest req) {
        Member m = find(id);
        if (!m.getEmail().equalsIgnoreCase(req.email()) && members.existsByEmailIgnoreCase(req.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu e-posta ile kayıtlı bir üye var");
        }
        req.applyTo(m);
        return members.save(m);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        // Members are deactivated rather than removed so loan history stays intact.
        Member m = find(id);
        m.setStatus(Member.Status.PASSIVE);
        members.save(m);
    }

    private Member find(String id) {
        return members.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Üye bulunamadı"));
    }
}
