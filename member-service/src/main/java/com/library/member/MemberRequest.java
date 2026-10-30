package com.library.member;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MemberRequest(
        @NotBlank String firstName,
        @NotBlank String lastName,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "^[+0-9 ()-]{7,20}$", message = "Geçerli bir telefon numarası girin") String phone,
        @Valid Member.Address address,
        Member.Status status,
        Boolean emailNotifications) {

    void applyTo(Member m) {
        m.setFirstName(firstName.trim());
        m.setLastName(lastName.trim());
        m.setEmail(email.trim().toLowerCase());
        m.setPhone(phone.trim());
        m.setAddress(address);
        if (status != null) m.setStatus(status);
        if (emailNotifications != null) m.setEmailNotifications(emailNotifications);
    }
}
