package com.library.member;

import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class SeedData {

    @Bean
    ApplicationRunner seedMembers(MemberRepository members) {
        return args -> {
            if (members.count() > 0) return;
            members.save(member("Ayşe", "Yılmaz", "ayse.yilmaz@example.com", "+90 532 111 22 33",
                    new Member.Address("Atatürk Cad. No:12", "Kadıköy", "İstanbul", "34710", "Türkiye")));
            members.save(member("Mehmet", "Demir", "mehmet.demir@example.com", "+90 533 444 55 66",
                    new Member.Address("Kızılay Sok. No:5", "Çankaya", "Ankara", "06420", "Türkiye")));
            members.save(member("Zeynep", "Kaya", "zeynep.kaya@example.com", "+90 535 777 88 99",
                    new Member.Address("Kordon Boyu No:44", "Konak", "İzmir", "35250", "Türkiye")));
        };
    }

    private static Member member(String first, String last, String email, String phone, Member.Address address) {
        Member m = new Member();
        m.setFirstName(first);
        m.setLastName(last);
        m.setEmail(email);
        m.setPhone(phone);
        m.setAddress(address);
        return m;
    }
}
