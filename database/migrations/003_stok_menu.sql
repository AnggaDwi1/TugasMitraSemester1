-- Migration: Tambah kolom stok ke tabel menu
-- Jalankan di phpMyAdmin → tab SQL → paste → Go

ALTER TABLE `menu` ADD COLUMN `stok` int NOT NULL DEFAULT 100 AFTER `status`;

-- Set stok awal untuk semua menu yang sudah ada
UPDATE `menu` SET `stok` = 100;

