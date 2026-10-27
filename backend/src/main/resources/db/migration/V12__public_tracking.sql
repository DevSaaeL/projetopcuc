ALTER TABLE chamados ADD COLUMN acompanhamento_token VARCHAR(36) UNIQUE;
CREATE INDEX chamados_local_assunto_status ON chamados(filial_id,bloco,sala,assunto_id,status);
