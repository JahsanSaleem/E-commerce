package com.hardwarestore.hardwarestore.service;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import java.nio.file.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.UUID;
@Service public class ProductImageStorage {
 private final Path directory;
 private final CloudDeliveryClient cloud;
 @org.springframework.beans.factory.annotation.Autowired
 public ProductImageStorage(@Value("${store.upload-dir:uploads}") String directory, CloudDeliveryClient cloud){this.directory=Path.of(directory).toAbsolutePath().normalize();this.cloud=cloud;}
 public ProductImageStorage(String directory){this(directory,null);}
 public String store(MultipartFile file) {
  if(file.isEmpty()||file.getSize()>5*1024*1024)throw new IllegalArgumentException("Choose a JPEG or PNG image up to 5 MB.");
  try(var input=ImageIO.createImageInputStream(file.getInputStream())) {
   var readers=ImageIO.getImageReaders(input);
   if(!readers.hasNext())throw new IllegalArgumentException("The file is not a supported image.");
   var reader=readers.next();
   try {
    if(!java.util.Set.of("JPEG","PNG").contains(reader.getFormatName().toUpperCase(java.util.Locale.ROOT)))throw new IllegalArgumentException("Only JPEG and PNG images are accepted.");
    reader.setInput(input);
    if((long)reader.getWidth(0)*reader.getHeight(0)>20000000)throw new IllegalArgumentException("Image dimensions are too large.");
    var image=reader.read(0);
    if(cloud!=null && cloud.usesCloudImages()) {
     var png=new ByteArrayOutputStream();ImageIO.write(image,"png",png);
     if(png.size()>5*1024*1024)throw new IllegalArgumentException("The decoded image is too large. Choose a smaller image.");
     return cloud.uploadImage(png.toByteArray());
    }
    Files.createDirectories(directory);String name=UUID.randomUUID()+".png";
    // Decode and re-encode to discard metadata and arbitrary appended payloads.
    ImageIO.write(image,"png",directory.resolve(name).toFile());return "/api/media/"+name;
   } finally {reader.dispose();}
  }catch(IOException failure){throw new IllegalArgumentException("Unable to read or store the image.");}
 }
 public org.springframework.core.io.Resource load(String name) {
  if(!name.matches("[0-9a-f-]{36}\\.png"))throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Image not found.");
  var file=new org.springframework.core.io.FileSystemResource(directory.resolve(name));
  if(!file.exists())throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,"Image not found.");return file;
 }
}
