-- Jalankan sekali pada database yang digunakan aplikasi.
-- Transaksi Midtrans harus tetap berstatus pending sampai statusnya diverifikasi.

ALTER TABLE pembayaran
  MODIFY COLUMN metode ENUM('cash', 'qris', 'transfer', 'ewallet', 'midtrans')
  DEFAULT 'cash';

CREATE TABLE IF NOT EXISTS midtrans_payment (
  id_midtrans_payment INT NOT NULL AUTO_INCREMENT,
  id_transaksi INT NOT NULL,
  order_id VARCHAR(64) NOT NULL,
  snap_token VARCHAR(255) DEFAULT NULL,
  redirect_url TEXT DEFAULT NULL,
  transaction_status VARCHAR(32) NOT NULL DEFAULT 'pending',
  payment_type VARCHAR(32) DEFAULT NULL,
  fraud_status VARCHAR(32) DEFAULT NULL,
  waktu_dibuat DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  waktu_diperbarui DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id_midtrans_payment),
  UNIQUE KEY uq_midtrans_order_id (order_id),
  UNIQUE KEY uq_midtrans_id_transaksi (id_transaksi),
  CONSTRAINT fk_midtrans_transaksi
    FOREIGN KEY (id_transaksi) REFERENCES transaksi (id_transaksi)
    ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
