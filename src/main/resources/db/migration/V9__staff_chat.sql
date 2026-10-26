-- Internal chat between Doctor(s) and Reception (one shared staff channel)
CREATE TABLE chat_messages (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  sender_id BIGINT NOT NULL,
  body VARCHAR(1000) NOT NULL,
  deleted TINYINT(1) NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_chat_sender FOREIGN KEY (sender_id) REFERENCES users(id),
  INDEX idx_chat_updated (updated_at),
  INDEX idx_chat_sender_time (sender_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE chat_reads (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT NOT NULL,
  last_read_message_id BIGINT NOT NULL DEFAULT 0,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uq_chat_read_user UNIQUE (user_id),
  CONSTRAINT fk_chat_read_user FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
