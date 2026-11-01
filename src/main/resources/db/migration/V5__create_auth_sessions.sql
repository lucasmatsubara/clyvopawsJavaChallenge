
CREATE TABLE tb_auth_session (
                                 id VARCHAR2(36) PRIMARY KEY,
                                 user_id NUMBER NOT NULL,
                                 refresh_atual VARCHAR2(64) NOT NULL,
                                 credencial_hash VARCHAR2(64) NOT NULL,
                                 expira_em NUMBER(19) NOT NULL,
                                 revogada NUMBER(1) DEFAULT 0 NOT NULL,
                                 CONSTRAINT fk_auth_session_user FOREIGN KEY (user_id) REFERENCES tb_user(id) ON DELETE CASCADE,
                                 CONSTRAINT ck_auth_session_revogada CHECK (revogada IN (0, 1))
);
CREATE INDEX ix_auth_session_user ON tb_auth_session(user_id);
CREATE INDEX ix_auth_session_expira ON tb_auth_session(expira_em);

CREATE TABLE tb_refresh_token (
                                  token_hash VARCHAR2(64) PRIMARY KEY,
                                  sessao_id VARCHAR2(36) NOT NULL,
                                  CONSTRAINT fk_refresh_session FOREIGN KEY (sessao_id) REFERENCES tb_auth_session(id) ON DELETE CASCADE
);
CREATE INDEX ix_refresh_session ON tb_refresh_token(sessao_id);