package com.hardwarestore.hardwarestore.controller;
import com.hardwarestore.hardwarestore.service.ProductImageStorage;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
@RestController public class ProductImageController {
 private final ProductImageStorage images;
 public ProductImageController(ProductImageStorage images){this.images=images;}
 @PostMapping("/api/products/images") public java.util.Map<String,String> upload(@RequestParam("file") MultipartFile file){return java.util.Map.of("imageUrl",images.store(file));}
 @GetMapping("/api/media/{name}") public ResponseEntity<org.springframework.core.io.Resource> image(@PathVariable String name){return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).header("X-Content-Type-Options","nosniff").cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(30))).body(images.load(name));}
}
