package club.taekwondo.repository.mongo;

import club.taekwondo.entity.mongo.Actualite;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActualiteRepository extends MongoRepository<Actualite, String> {
    List<Actualite> findByIsFeaturedTrue();
}
