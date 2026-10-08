package br.edu.helpdesk;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.*;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:profile-photo;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=photo@test.invalid","app.bootstrap-password=OnlyForTests-123456"})
@AutoConfigureMockMvc
class ProfilePhotoTest {
 @Autowired MockMvc mvc;
 @Test void savesNormalizesAndProtectsProfilePhoto()throws Exception {
  var session=(MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"photo@test.invalid\",\"password\":\"OnlyForTests-123456\"}")).andExpect(status().isOk()).andReturn().getRequest().getSession();
  mvc.perform(get("/api/auth/foto").session(session)).andExpect(status().isNotFound());
  var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(1024,768,BufferedImage.TYPE_INT_RGB),"png",bytes);
  var file=new MockMultipartFile("arquivo","foto.png","image/png",bytes.toByteArray());
  mvc.perform(multipart("/api/auth/foto").file(file).session(session)).andExpect(status().isForbidden());
  mvc.perform(multipart("/api/auth/foto").file(file).session(session).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.foto_versao").isNotEmpty());
  var response=mvc.perform(get("/api/auth/foto").session(session)).andExpect(status().isOk()).andExpect(content().contentType("image/png")).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse();
  var image=ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));assertEquals(512,image.getWidth());assertEquals(384,image.getHeight());
  mvc.perform(get("/api/auth/me").session(session)).andExpect(jsonPath("$.foto_versao").isNotEmpty()).andExpect(jsonPath("$.foto").doesNotExist());
  mvc.perform(get("/api/auth/foto")).andExpect(status().isUnauthorized());
  mvc.perform(multipart("/api/auth/foto").file(new MockMultipartFile("arquivo","fake.png","image/png","<svg></svg>".getBytes())).session(session).with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(multipart("/api/auth/foto").file(new MockMultipartFile("arquivo","large.png","image/png",new byte[2*1024*1024+1])).session(session).with(csrf())).andExpect(status().isBadRequest());
  mvc.perform(get("/api/auth/foto").session(session)).andExpect(content().bytes(response.getContentAsByteArray()));
 }
}
