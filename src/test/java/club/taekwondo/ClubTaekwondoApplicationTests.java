package club.taekwondo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import club.taekwondo.service.jpa.ActualiteService;
import club.taekwondo.service.jpa.GalerieService;

@SpringBootTest
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
