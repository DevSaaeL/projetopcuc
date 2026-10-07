ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_perfil_check;
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS constraint_5;
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS constraint_6;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_filial_check CHECK (perfil = 'MASTER_ADMIN' OR perfil = 'USUARIO' OR filial_id IS NOT NULL);
