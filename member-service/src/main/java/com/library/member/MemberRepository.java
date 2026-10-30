package com.library.member;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MemberRepository extends MongoRepository<Member, String> {
    boolean existsByEmailIgnoreCase(String email);

    @Query("{ $or: [ { firstName: { $regex: ?0, $options: 'i' } }, { lastName: { $regex: ?0, $options: 'i' } }, "
            + "{ email: { $regex: ?0, $options: 'i' } }, { phone: { $regex: ?0, $options: 'i' } } ] }")
    List<Member> search(String text);
}
