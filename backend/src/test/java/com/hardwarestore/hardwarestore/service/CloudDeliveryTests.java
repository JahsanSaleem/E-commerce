package com.hardwarestore.hardwarestore.service;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.mock.web.MockMultipartFile;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class CloudDeliveryTests {
    @Test void emailUsesHttpsApiAndHidesProviderFailure() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var cloud = new CloudDeliveryClient(builder, "brevo-api", "test-api-key", "store@example.com",
                "local", "", "", "");
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("api-key", "test-api-key"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("textContent")))
                .andRespond(withSuccess("{\"messageId\":\"test\"}", MediaType.APPLICATION_JSON));
        cloud.sendEmail("customer@example.com", "Verify", "code: 123456");
        server.verify();
        server.reset();
        server.expect(requestTo("https://api.brevo.com/v3/smtp/email"))
                .andRespond(withStatus(org.springframework.http.HttpStatus.UNAUTHORIZED).body("private provider response"));
        var error = assertThrows(ResponseStatusException.class,
                () -> cloud.sendEmail("customer@example.com", "Verify", "code: 123456"));
        assertEquals(503, error.getStatusCode().value());
        assertFalse(error.getReason().contains("private"));
        server.verify();
    }

    @Test void cloudUploadsDecodeImagesAndDoNotAcceptFakeFiles() throws Exception {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        var cloud = new CloudDeliveryClient(builder, "smtp", "", "", "cloudinary", "demo", "test-key", "test-secret");
        var storage = new ProductImageStorage("unused-test-path", cloud);
        var bytes = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new BufferedImage(2,2,BufferedImage.TYPE_INT_RGB), "png", bytes);
        server.expect(requestTo("https://api.cloudinary.com/v1_1/demo/image/upload"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Basic dGVzdC1rZXk6dGVzdC1zZWNyZXQ="))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("mustafa-products")))
                .andRespond(withSuccess("{\"secure_url\":\"https://res.cloudinary.com/demo/image/upload/product.png\"}", MediaType.APPLICATION_JSON));
        assertEquals("https://res.cloudinary.com/demo/image/upload/product.png", storage.store(
                new MockMultipartFile("file", "image.png", "image/png", bytes.toByteArray())));
        assertThrows(IllegalArgumentException.class, () -> storage.store(
                new MockMultipartFile("file", "fake.png", "image/png", "bad".getBytes())));
        server.verify();
    }

    @Test void missingCloudCredentialsDoNotFallBackToEphemeralStorage() {
        var cloud = new CloudDeliveryClient(RestClient.builder(), "brevo-api", "", "", "cloudinary", "demo", "", "");
        assertThrows(ResponseStatusException.class, () -> cloud.sendEmail("customer@example.com", "subject", "body"));
        assertThrows(ResponseStatusException.class, () -> cloud.uploadImage(new byte[]{1}));
    }
}
