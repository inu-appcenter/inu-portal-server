CREATE TABLE feature_flag (
                              id BIGINT NOT NULL AUTO_INCREMENT,
                              flag_key VARCHAR(100) NOT NULL,
                              enabled TINYINT(1) NOT NULL,
                              description VARCHAR(255) NULL,
                              client_visible TINYINT(1) NOT NULL,
                              create_date DATE NULL,
                              modified_date DATETIME NULL,
                              PRIMARY KEY (id),
                              CONSTRAINT uk_feature_flag_key UNIQUE (flag_key)
);
