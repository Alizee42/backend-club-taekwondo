package club.taekwondo.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import club.taekwondo.entity.jpa.Club;

@Repository
public interface ClubRepository extends JpaRepository<Club, Long> {
    Club findByName(String name);
}
