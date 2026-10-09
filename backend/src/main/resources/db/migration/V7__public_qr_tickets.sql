ALTER TABLE chamados ALTER COLUMN solicitante_id DROP NOT NULL;
ALTER TABLE chamados ADD COLUMN solicitante_nome VARCHAR(160);
ALTER TABLE chamados ADD COLUMN requisicao_id VARCHAR(36) UNIQUE;
ALTER TABLE auditoria ALTER COLUMN autor_id DROP NOT NULL;
ALTER TABLE auditoria ADD COLUMN autor_nome VARCHAR(160);
