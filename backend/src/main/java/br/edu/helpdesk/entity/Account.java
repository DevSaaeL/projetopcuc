package br.edu.helpdesk.entity;
public record Account(long id, String nome, String email, String senhaHash, String perfil, Long filialId, boolean ativo, int versaoSessao) {
 public boolean master() { return perfil.equals("MASTER_ADMIN"); }
 public boolean admin() { return master() || perfil.equals("ADMIN"); }
 public boolean support() { return admin() || perfil.equals("SUPORTE"); }
}
