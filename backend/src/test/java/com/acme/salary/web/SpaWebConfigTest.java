package com.acme.salary.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Uses the test stand-in at src/test/resources/static/index.html. */
@SpringBootTest
@AutoConfigureMockMvc
class SpaWebConfigTest {

	@Autowired
	private MockMvcTester mvc;

	@Test
	void rootServesTheAngularShell() {
		// Spring Boot's welcome-page support forwards "/" to index.html (MockMvc doesn't follow forwards).
		assertThat(mvc.get().uri("/")).hasStatus(HttpStatus.OK).hasForwardedUrl("index.html");
	}

	@Test
	void deepLinksFallBackToTheShellSoRefreshWorks() {
		assertThat(mvc.get().uri("/employees/7/edit")).hasStatus(HttpStatus.OK).bodyText().contains("spa-shell");
		assertThat(mvc.get().uri("/insights?country=US")).hasStatus(HttpStatus.OK).bodyText().contains("spa-shell");
	}

	@Test
	void unknownApiPathsStayA404() {
		assertThat(mvc.get().uri("/api/does-not-exist")).hasStatus(HttpStatus.NOT_FOUND);
	}

	@Test
	void apiStillWorks() {
		assertThat(mvc.get().uri("/api/meta/filters")).hasStatus(HttpStatus.OK);
	}

}
