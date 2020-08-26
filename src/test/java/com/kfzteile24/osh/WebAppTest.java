package com.kfzteile24.osh;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.assertTrue;

/**
 * @author Svetlana Dorokhova.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
        classes = SohProcessApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class WebAppTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Test
    public void testAdminEndpointAvailable() {
        ResponseEntity<String> response =
                testRestTemplate.getForEntity("/camunda/app/admin/", String.class);

        assertTrue(response.getStatusCode().is2xxSuccessful());
    }

}

