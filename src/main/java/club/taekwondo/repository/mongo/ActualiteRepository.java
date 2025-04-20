package club.taekwondo.repository.mongo;

import club.taekwondo.entity.mongo.Actualite;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ActualiteRepository extends MongoRepository<Actualite, String> {
}
