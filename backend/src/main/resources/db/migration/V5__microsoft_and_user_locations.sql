ALTER TABLE usuarios ADD COLUMN microsoft_object_id VARCHAR(36);
CREATE UNIQUE INDEX usuarios_microsoft_object ON usuarios(microsoft_object_id);
CREATE TABLE usuario_filiais (
 usuario_id BIGINT NOT NULL REFERENCES usuarios(id),
 filial_id BIGINT NOT NULL REFERENCES filiais(id),
 PRIMARY KEY (usuario_id, filial_id)
);
INSERT INTO usuario_filiais(usuario_id,filial_id)
 SELECT id,filial_id FROM usuarios WHERE filial_id IS NOT NULL;
CREATE INDEX usuario_filiais_filial ON usuario_filiais(filial_id,usuario_id);
