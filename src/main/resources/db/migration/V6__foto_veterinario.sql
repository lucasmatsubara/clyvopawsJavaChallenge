
ALTER TABLE tb_foto ADD (veterinario_id NUMBER);

ALTER TABLE tb_foto DROP CONSTRAINT ck_foto_dono;

ALTER TABLE tb_foto ADD CONSTRAINT uq_foto_veterinario UNIQUE (veterinario_id);

ALTER TABLE tb_foto ADD CONSTRAINT fk_foto_veterinario
    FOREIGN KEY (veterinario_id) REFERENCES tb_veterinario(id) ON DELETE CASCADE;

ALTER TABLE tb_foto ADD CONSTRAINT ck_foto_dono CHECK (
    (CASE WHEN tutor_id IS NULL THEN 0 ELSE 1 END
        + CASE WHEN pet_id IS NULL THEN 0 ELSE 1 END
        + CASE WHEN veterinario_id IS NULL THEN 0 ELSE 1 END) = 1
    );