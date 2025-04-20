
package club.taekwondo.repository.mongo;

import club.taekwondo.entity.mongo.Galerie;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GalerieRepository extends MongoRepository<Galerie, String> {
}
