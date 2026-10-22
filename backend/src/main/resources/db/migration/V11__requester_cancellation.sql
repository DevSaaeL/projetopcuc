ALTER TABLE chamados ADD COLUMN chave_cancelamento_hash VARCHAR(64);
ALTER TABLE chamados ADD COLUMN cancelado_por_solicitante BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE chamados ADD COLUMN motivo_cancelamento VARCHAR(1000) NOT NULL DEFAULT '';
ALTER TABLE atendimentos_chamado ADD COLUMN cancelado_por_solicitante BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE atendimentos_chamado ADD COLUMN motivo_cancelamento VARCHAR(1000) NOT NULL DEFAULT '';
