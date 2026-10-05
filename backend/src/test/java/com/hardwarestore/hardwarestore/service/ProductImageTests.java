package com.hardwarestore.hardwarestore.service;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.Path;
import java.io.ByteArrayOutputStream;
import static org.junit.jupiter.api.Assertions.*;
class ProductImageTests {
 @TempDir Path directory;
 @Test void decodesStoresAndReadsImageWithoutTrustingFilename()throws Exception {
  var bytes=new ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
  var storage=new ProductImageStorage(directory.toString());var url=storage.store(new MockMultipartFile("file","../../bad.html","text/html",bytes.toByteArray()));
  assertTrue(url.startsWith("/api/media/"));assertTrue(storage.load(url.substring(url.lastIndexOf('/')+1)).exists());
  assertThrows(org.springframework.web.server.ResponseStatusException.class,()->storage.load("../../secret"));
  assertThrows(IllegalArgumentException.class,()->storage.store(new MockMultipartFile("file","fake.png","image/png","<script>bad</script>".getBytes())));
 }
}
