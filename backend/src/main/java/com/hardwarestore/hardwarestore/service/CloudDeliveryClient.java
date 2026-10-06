package com.hardwarestore.hardwarestore.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class CloudDeliveryClient {
    private final RestClient http;
    private final String mailProvider, apiKey, from, imageProvider, cloudName, cloudKey, cloudSecret;

    @org.springframework.beans.factory.annotation.Autowired
    public CloudDeliveryClient(
            @Value("${store.mail.provider:smtp}") String mailProvider,
            @Value("${store.mail.brevo-api-key:}") String apiKey,
            @Value("${store.mail.from:}") String from,
            @Value("${store.images.provider:local}") String imageProvider,
            @Value("${store.images.cloudinary.cloud-name:}") String cloudName,
            @Value("${store.images.cloudinary.api-key:}") String cloudKey,
            @Value("${store.images.cloudinary.api-secret:}") String cloudSecret) {
        this(clientBuilder(), mailProvider, apiKey, from, imageProvider, cloudName, cloudKey, cloudSecret);
    }

    CloudDeliveryClient(RestClient.Builder builder, String mailProvider, String apiKey, String from,
                        String imageProvider, String cloudName, String cloudKey, String cloudSecret) {
        this.http = builder.build();
        this.mailProvider = mailProvider; this.apiKey = apiKey; this.from = from;
        this.imageProvider = imageProvider; this.cloudName = cloudName;
        this.cloudKey = cloudKey; this.cloudSecret = cloudSecret;
        if (!List.of("smtp", "brevo-api").contains(mailProvider)
                || !List.of("local", "cloudinary").contains(imageProvider)) {
            throw new IllegalArgumentException("Unsupported email or image provider.");
        }
    }

    private static RestClient.Builder clientBuilder() {
        var factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)).build());
        factory.setReadTimeout(Duration.ofSeconds(20));
        return RestClient.builder().requestFactory(factory);
    }

    public boolean usesEmailApi() { return mailProvider.equals("brevo-api"); }
    public boolean usesCloudImages() { return imageProvider.equals("cloudinary"); }

    public void sendEmail(String email, String subject, String body) {
        if (apiKey.isBlank() || from.isBlank()) throw unavailable("Email delivery is not configured yet.");
        try {
            http.post().uri("https://api.brevo.com/v3/smtp/email")
                    .header("api-key", apiKey).contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("sender", Map.of("email", from, "name", "Mustafa Hardware"),
                            "to", List.of(Map.of("email", email)), "subject", subject, "textContent", body))
                    .retrieve().toBodilessEntity();
        } catch (RestClientException failure) {
            // Provider responses can contain secrets or message content: do not expose them.
            throw unavailable("We could not send the email. Please try again later.");
        }
    }

    public String uploadImage(byte[] png) {
        if (!cloudName.matches("[A-Za-z0-9_-]+") || cloudKey.isBlank() || cloudSecret.isBlank())
            throw unavailable("Product image storage is not configured yet.");
        var form = new LinkedMultiValueMap<String, Object>();
        form.add("file", new ByteArrayResource(png) {
            @Override public String getFilename() { return "product.png"; }
        });
        form.add("folder", "mustafa-products");
        try {
            var response = http.post().uri("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload")
                    .headers(headers -> headers.setBasicAuth(cloudKey, cloudSecret))
                    .contentType(MediaType.MULTIPART_FORM_DATA).body(form).retrieve().body(Map.class);
            Object value = response == null ? null : response.get("secure_url");
            if (!(value instanceof String url)) throw unavailable("Image upload failed. Please try again.");
            var uri = URI.create(url);
            if (!"https".equals(uri.getScheme()) || !"res.cloudinary.com".equals(uri.getHost())
                    || uri.getUserInfo() != null) throw unavailable("Image upload failed. Please try again.");
            return url;
        } catch (RestClientException | IllegalArgumentException failure) {
            throw unavailable("Image upload failed. Please try again.");
        }
    }

    private static ResponseStatusException unavailable(String message) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }
}
