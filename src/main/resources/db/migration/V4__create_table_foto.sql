
CREATE TABLE tb_foto (
                         id VARCHAR2(36) NOT NULL,
                         conteudo BLOB NOT NULL,
                         tutor_id NUMBER,
                         pet_id NUMBER,
                         criada_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
                         CONSTRAINT pk_foto PRIMARY KEY (id),
                         CONSTRAINT uq_foto_tutor UNIQUE (tutor_id),
                         CONSTRAINT uq_foto_pet UNIQUE (pet_id),
                         CONSTRAINT ck_foto_dono CHECK (
                             (tutor_id IS NOT NULL AND pet_id IS NULL)
                                 OR (tutor_id IS NULL AND pet_id IS NOT NULL)
                             ),
                         CONSTRAINT fk_foto_tutor FOREIGN KEY (tutor_id) REFERENCES tb_tutor(id) ON DELETE CASCADE,
                         CONSTRAINT fk_foto_pet FOREIGN KEY (pet_id) REFERENCES tb_pet(id) ON DELETE CASCADE
);