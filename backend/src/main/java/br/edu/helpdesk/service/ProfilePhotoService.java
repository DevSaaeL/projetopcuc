package br.edu.helpdesk.service;

import br.edu.helpdesk.repository.DeskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

@Service
public class ProfilePhotoService {
 private final DeskRepository repo;
 private final DeskService desk;
 public ProfilePhotoService(DeskRepository repo,DeskService desk){this.repo=repo;this.desk=desk;}
 @Transactional
 public Map<String,String> save(MultipartFile file)throws IOException {
  long id=desk.current().id();
  if(file.isEmpty()||file.getSize()>2*1024*1024)throw DeskService.bad("Escolha uma foto JPG ou PNG de até 2 MB.");
  BufferedImage source;
  try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(file.getBytes()))){
   var readers=ImageIO.getImageReaders(input);
   if(!readers.hasNext())throw DeskService.bad("A foto deve ser uma imagem JPG ou PNG válida.");
   var reader=readers.next();
   try {
    reader.setInput(input);
    if(!Set.of("JPEG","PNG").contains(reader.getFormatName().toUpperCase(Locale.ROOT)))throw DeskService.bad("Use uma foto JPG ou PNG.");
    int width=reader.getWidth(0),height=reader.getHeight(0);
    if(width<1||height<1||width>6000||height>6000||(long)width*height>12000000)throw DeskService.bad("A foto deve ter até 12 megapixels e 6000 pixels por lado.");
    source=reader.read(0);
   }finally{reader.dispose();}
  }catch(IOException|IllegalArgumentException e){throw DeskService.bad("Não foi possível ler a foto. Escolha um JPG ou PNG válido.");}
  double scale=Math.min(1d,512d/Math.max(source.getWidth(),source.getHeight()));
  var image=new BufferedImage(Math.max(1,(int)(source.getWidth()*scale)),Math.max(1,(int)(source.getHeight()*scale)),BufferedImage.TYPE_INT_ARGB);
  var graphics=image.createGraphics();
  try{graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);graphics.drawImage(source,0,0,image.getWidth(),image.getHeight(),null);}finally{graphics.dispose();}
  var output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);
  String version=UUID.randomUUID().toString();
  repo.update("UPDATE usuarios SET foto=?,foto_versao=? WHERE id=?",output.toByteArray(),version,id);
  return Map.of("foto_versao",version);
 }
 public byte[] read(){var row=repo.one("SELECT foto FROM usuarios WHERE id=?",desk.current().id());if(row.get("foto")==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Conta sem foto.");return (byte[])row.get("foto");}
}
