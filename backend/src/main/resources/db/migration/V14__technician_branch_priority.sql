ALTER TABLE usuario_filiais ADD COLUMN prioritario BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX usuario_filiais_prioridade ON usuario_filiais(usuario_id,prioritario,filial_id);
