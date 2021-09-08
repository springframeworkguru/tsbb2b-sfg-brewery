package guru.springframework.brewery.web.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import guru.springframework.brewery.web.model.BeerPagedList;

import static org.assertj.core.api.Java6Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BeerControllerIT {
	
	@Autowired
	private TestRestTemplate restTemplate;
	
	@Test
	void testListBeers() {
		
		BeerPagedList beerPagedList = restTemplate.getForObject("/api/v1/beer", BeerPagedList.class);
		
		assertThat(beerPagedList.getContent()).hasSize(3);
		
	}

}
