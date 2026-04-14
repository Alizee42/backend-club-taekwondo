package club.taekwondo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import club.taekwondo.service.mongo.ActualiteService;
import club.taekwondo.service.mongo.GalerieService;

@SpringBootTest(properties = {
		"spring.autoconfigure.exclude="
				+ "org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration,"
				+ "org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
class ClubTaekwondoApplicationTests {

	@MockBean
	private ActualiteService actualiteService;

	@MockBean
	private GalerieService galerieService;

	@Test
	void contextLoads() {
	}

}
