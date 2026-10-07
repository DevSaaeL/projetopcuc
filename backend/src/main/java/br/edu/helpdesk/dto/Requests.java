package br.edu.helpdesk.dto;
import jakarta.validation.constraints.*;
public final class Requests {
 public record Login(@NotBlank @Size(max=200) String username,@NotBlank @Size(max=72) String password){}
 public record Register(@Email @NotBlank @Size(max=200) String email,@NotBlank @Size(min=12,max=72) String senha){}
 public record Branch(@NotBlank @Size(max=160) String nome,@NotBlank @Size(max=120) String cidade,@Pattern(regexp="[A-Z]{2}") @NotNull String estado,@Size(max=300) String endereco,@Size(max=160) String responsavel,@Size(max=40) String telefone,boolean ativo){}
 public record User(@NotBlank @Size(max=160) String nome,@Email @NotBlank @Size(max=200) String email,@Size(max=72) String senha,@NotBlank String perfil,Long filialId,boolean ativo){}
 public record Qr(@NotBlank @Size(max=160) String nome,@NotNull Long filialId,@NotBlank @Size(max=80) String bloco,@NotBlank @Size(max=80) String sala,@Size(max=300) String descricao){}
 public record Ticket(@NotBlank @Size(max=150) String titulo,@NotBlank @Size(max=2000) String descricao,Long filialId,@Size(max=80) String bloco,@Size(max=80) String sala,@Size(max=100) String setor,@Size(max=80) String categoria,@Size(max=40) String tipoAtendimento,@NotBlank String prioridade,@Size(max=36) String qrId){}
 public record Message(@NotBlank @Size(max=4000) String mensagem){}
 public record Action(@NotBlank String acao,@Size(max=2000) String solucao){}
 public record Password(@NotBlank @Size(max=72) String atual,@NotBlank @Size(min=12,max=72) String nova){}
 public record Profile(@NotBlank @Size(max=160) String nome,@Email @NotBlank @Size(max=200) String email,@Size(max=40) String telefone){}
}
