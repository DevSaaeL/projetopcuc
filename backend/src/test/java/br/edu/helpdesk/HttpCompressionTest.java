package br.edu.helpdesk;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import java.net.URI;
import java.net.http.*;
import java.io.*;
import java.util.zip.GZIPInputStream;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"server.address=127.0.0.1","spring.datasource.url=jdbc:h2:mem:http-compression;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1","app.bootstrap-email=","app.bootstrap-password="})
class HttpCompressionTest {
 @LocalServerPort int port;
 @Test void largeStylesheetIsCompressedWithoutChangingItsContents()throws Exception{
  try(var client=HttpClient.newHttpClient()){
   var uri=URI.create("http://127.0.0.1:"+port+"/assets/css/style.css");
   var plain=client.send(HttpRequest.newBuilder(uri).build(),HttpResponse.BodyHandlers.ofByteArray());
   var gzip=client.send(HttpRequest.newBuilder(uri).header("Accept-Encoding","gzip").build(),HttpResponse.BodyHandlers.ofByteArray());
   assertEquals(200,plain.statusCode());assertEquals(200,gzip.statusCode());assertEquals("gzip",gzip.headers().firstValue("Content-Encoding").orElse(""));
   try(var decoded=new GZIPInputStream(new ByteArrayInputStream(gzip.body()))){assertArrayEquals(plain.body(),decoded.readAllBytes());}
   assertTrue(gzip.body().length<plain.body().length/2,"O CSS comprimido deve transferir menos da metade dos bytes.");
  }
 }
}
