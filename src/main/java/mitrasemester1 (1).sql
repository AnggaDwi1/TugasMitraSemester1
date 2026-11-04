-- phpMyAdmin SQL Dump
-- version 5.2.3
-- https://www.phpmyadmin.net/
--
-- Host: localhost:3306
-- Generation Time: Nov 04, 2026 at 01:49 AM
-- Server version: 8.0.30
-- PHP Version: 8.1.10

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `mitrasemester1`
--

-- --------------------------------------------------------

--
-- Table structure for table `kasir`
--

CREATE TABLE `kasir` (
  `id_kasir` int NOT NULL,
  `nama_kasir` varchar(50) NOT NULL,
  `uid_kartu` varchar(30) NOT NULL,
  `status` enum('aktif','nonaktif') DEFAULT 'aktif'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `kasir`
--

INSERT INTO `kasir` (`id_kasir`, `nama_kasir`, `uid_kartu`, `status`) VALUES
(1, 'Pak Owo', '123', 'aktif');

-- --------------------------------------------------------

--
-- Table structure for table `kategori_menu`
--

CREATE TABLE `kategori_menu` (
  `id_kategori` int NOT NULL,
  `nama_kategori` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `kategori_menu`
--

INSERT INTO `kategori_menu` (`id_kategori`, `nama_kategori`) VALUES
(1, 'Makanan');

-- --------------------------------------------------------

--
-- Table structure for table `log_kasir`
--

CREATE TABLE `log_kasir` (
  `id_log` int NOT NULL,
  `id_kasir` int DEFAULT NULL,
  `waktu_login` datetime DEFAULT CURRENT_TIMESTAMP,
  `waktu_logout` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `log_kasir`
--

INSERT INTO `log_kasir` (`id_log`, `id_kasir`, `waktu_login`, `waktu_logout`) VALUES
(1, 1, '2026-11-02 10:39:05', '2026-11-02 10:39:11'),
(2, 1, '2026-11-02 10:39:14', '2026-11-02 10:39:19'),
(3, 1, '2026-11-02 10:39:24', '2026-11-02 10:39:31'),
(4, 1, '2026-11-02 10:39:33', NULL),
(5, 1, '2026-11-02 10:42:27', '2026-11-02 10:43:15'),
(6, 1, '2026-11-02 10:43:17', NULL),
(7, 1, '2026-11-02 10:46:27', '2026-11-02 10:47:25'),
(8, 1, '2026-11-02 10:47:30', NULL),
(9, 1, '2026-11-02 10:56:44', '2026-11-02 10:56:50'),
(10, 1, '2026-11-02 10:56:55', NULL),
(11, 1, '2026-11-02 16:11:35', NULL),
(12, 1, '2026-11-02 16:27:43', NULL),
(13, 1, '2026-11-02 16:31:03', NULL),
(14, 1, '2026-11-02 16:35:05', NULL),
(15, 1, '2026-11-02 16:42:58', NULL),
(16, 1, '2026-11-03 11:35:49', NULL),
(17, 1, '2026-11-03 11:45:08', NULL),
(18, 1, '2026-11-03 11:52:03', NULL),
(19, 1, '2026-11-04 08:43:57', NULL);

-- --------------------------------------------------------

--
-- Table structure for table `menu`
--

CREATE TABLE `menu` (
  `id_menu` int NOT NULL,
  `id_kategori` int DEFAULT NULL,
  `nama_menu` varchar(100) NOT NULL,
  `harga` decimal(10,2) NOT NULL,
  `deskripsi` text,
  `status` enum('tersedia','habis') DEFAULT 'tersedia'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `menu`
--

INSERT INTO `menu` (`id_menu`, `id_kategori`, `nama_menu`, `harga`, `deskripsi`, `status`) VALUES
(1, 1, 'Ayam Pok Pok', 10000.00, 'Ayam crispy potongan kecil', 'tersedia'),
(2, 1, 'Tahu Walik', 10000.00, 'Tahu walik isi aci gurih', 'tersedia'),
(3, 1, 'Kentang Goreng', 8000.00, 'Kentang goreng crispy', 'tersedia'),
(4, 1, 'Cireng', 8000.00, 'Aci goreng kenyal', 'tersedia'),
(5, 1, 'Mie Chikmong', 8000.00, 'Mie pedas ala chikmong', 'tersedia');

-- --------------------------------------------------------

--
-- Table structure for table `menu_varian`
--

CREATE TABLE `menu_varian` (
  `id_menu` int NOT NULL,
  `id_rasa` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `menu_varian`
--

INSERT INTO `menu_varian` (`id_menu`, `id_rasa`) VALUES
(1, 1),
(2, 1),
(3, 1),
(4, 1),
(5, 1),
(1, 2),
(2, 2),
(3, 2),
(4, 2),
(5, 2),
(1, 3),
(2, 3),
(3, 3),
(4, 3),
(5, 3),
(1, 4),
(2, 4),
(3, 4),
(4, 4),
(5, 4),
(1, 5),
(2, 5),
(3, 5),
(4, 5),
(5, 5),
(1, 6),
(2, 6),
(3, 6),
(4, 6),
(5, 6),
(1, 7),
(2, 7),
(3, 7),
(4, 7),
(5, 7),
(1, 8),
(2, 8),
(3, 8),
(4, 8),
(5, 8),
(1, 9),
(2, 9),
(3, 9),
(4, 9),
(5, 9);

-- --------------------------------------------------------

--
-- Table structure for table `pembayaran`
--

CREATE TABLE `pembayaran` (
  `id_pembayaran` int NOT NULL,
  `id_transaksi` int NOT NULL,
  `metode` enum('cash','qris','transfer','ewallet') DEFAULT 'cash',
  `jumlah_bayar` decimal(12,2) NOT NULL,
  `kembalian` decimal(12,2) DEFAULT '0.00',
  `waktu_bayar` datetime DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `pembayaran`
--

INSERT INTO `pembayaran` (`id_pembayaran`, `id_transaksi`, `metode`, `jumlah_bayar`, `kembalian`, `waktu_bayar`) VALUES
(1, 3, 'cash', 50000.00, 26000.00, '2026-11-02 09:46:24'),
(2, 4, 'cash', 90000.00, 82000.00, '2026-11-02 09:57:42'),
(3, 5, 'cash', 90000.00, 80000.00, '2026-11-02 09:59:06'),
(4, 6, 'cash', 1000000.00, 10000.00, '2026-11-02 10:05:17'),
(5, 7, 'qris', 8000.00, 0.00, '2026-11-02 10:06:51'),
(6, 8, 'qris', 44000.00, 0.00, '2026-11-02 10:08:48'),
(7, 9, 'qris', 26000.00, 0.00, '2026-11-02 10:12:43'),
(8, 10, 'cash', 50000.00, 22000.00, '2026-11-03 11:51:45'),
(9, 11, 'qris', 10000.00, 0.00, '2026-11-03 11:51:45'),
(10, 12, 'cash', 30000.00, 6000.00, '2026-11-03 11:51:45'),
(11, 13, 'qris', 26000.00, 0.00, '2026-11-03 11:51:45'),
(12, 14, 'cash', 20000.00, 2000.00, '2026-11-03 11:51:45'),
(13, 15, 'qris', 40000.00, 0.00, '2026-11-03 11:51:45'),
(14, 16, 'cash', 50000.00, 18000.00, '2026-11-03 11:51:45'),
(15, 17, 'cash', 50000.00, 20000.00, '2026-11-03 11:51:45'),
(16, 18, 'qris', 32000.00, 0.00, '2026-11-03 11:51:45'),
(17, 19, 'cash', 10000.00, 0.00, '2026-11-03 11:51:45'),
(18, 20, 'cash', 116000.00, 0.00, '2026-11-03 11:56:40');

-- --------------------------------------------------------

--
-- Table structure for table `transaksi`
--

CREATE TABLE `transaksi` (
  `id_transaksi` int NOT NULL,
  `id_kasir` int DEFAULT NULL,
  `kode_transaksi` varchar(30) NOT NULL,
  `tanggal` datetime DEFAULT CURRENT_TIMESTAMP,
  `total_harga` decimal(12,2) DEFAULT '0.00',
  `status` enum('pending','selesai','batal') DEFAULT 'selesai'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `transaksi`
--

INSERT INTO `transaksi` (`id_transaksi`, `id_kasir`, `kode_transaksi`, `tanggal`, `total_harga`, `status`) VALUES
(3, NULL, 'TRX-20261102-001', '2026-11-02 09:46:24', 24000.00, 'selesai'),
(4, NULL, 'TRX-20261102-002', '2026-11-02 09:57:42', 8000.00, 'batal'),
(5, NULL, 'TRX-20261102-003', '2026-11-02 09:59:06', 10000.00, 'batal'),
(6, NULL, 'TRX-20261102-004', '2026-11-02 10:05:17', 990000.00, 'selesai'),
(7, NULL, 'TRX-20261102-005', '2026-11-02 10:06:51', 8000.00, 'selesai'),
(8, NULL, 'TRX-20261102-006', '2026-11-02 10:08:48', 44000.00, 'selesai'),
(9, NULL, 'TRX-20261102-007', '2026-11-02 10:12:43', 26000.00, 'selesai'),
(10, NULL, 'TRX-20261103-D01', '2026-11-03 06:51:45', 28000.00, 'selesai'),
(11, NULL, 'TRX-20261103-D02', '2026-11-03 07:21:45', 10000.00, 'selesai'),
(12, NULL, 'TRX-20261103-D03', '2026-11-03 07:51:45', 24000.00, 'selesai'),
(13, NULL, 'TRX-20261103-D04', '2026-11-03 08:31:45', 26000.00, 'selesai'),
(14, NULL, 'TRX-20261103-D05', '2026-11-03 09:21:45', 18000.00, 'selesai'),
(15, NULL, 'TRX-20261103-D06', '2026-11-03 10:21:45', 40000.00, 'selesai'),
(16, NULL, 'TRX-20261103-D07', '2026-11-03 11:11:45', 32000.00, 'selesai'),
(17, NULL, 'TRX-20261102-D08', '2026-11-02 11:51:45', 30000.00, 'selesai'),
(18, NULL, 'TRX-20261102-D09', '2026-11-02 10:51:45', 32000.00, 'selesai'),
(19, NULL, 'TRX-20261103-D10', '2026-11-03 11:31:45', 10000.00, 'batal'),
(20, NULL, 'TRX-20261103-009', '2026-11-03 11:56:40', 116000.00, 'selesai');

-- --------------------------------------------------------

--
-- Table structure for table `transaksi_detail`
--

CREATE TABLE `transaksi_detail` (
  `id_detail` int NOT NULL,
  `id_transaksi` int NOT NULL,
  `id_menu` int NOT NULL,
  `id_rasa` int DEFAULT NULL,
  `jumlah` int NOT NULL DEFAULT '1',
  `harga_satuan` decimal(10,2) NOT NULL,
  `subtotal` decimal(12,2) GENERATED ALWAYS AS ((`jumlah` * `harga_satuan`)) STORED,
  `catatan` varchar(150) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `transaksi_detail`
--

INSERT INTO `transaksi_detail` (`id_detail`, `id_transaksi`, `id_menu`, `id_rasa`, `jumlah`, `harga_satuan`, `catatan`) VALUES
(1, 3, 3, 3, 3, 8000.00, NULL),
(2, 4, 5, 1, 1, 8000.00, NULL),
(3, 5, 1, 1, 1, 10000.00, NULL),
(4, 6, 1, 2, 99, 10000.00, 'tahlil'),
(5, 7, 3, 1, 1, 8000.00, NULL),
(6, 8, 1, 1, 1, 10000.00, NULL),
(7, 8, 2, 1, 1, 10000.00, NULL),
(8, 8, 3, 1, 1, 8000.00, NULL),
(9, 8, 5, 1, 1, 8000.00, NULL),
(10, 8, 4, 1, 1, 8000.00, NULL),
(11, 9, 5, 1, 1, 8000.00, NULL),
(12, 9, 3, 1, 1, 8000.00, NULL),
(13, 9, 2, 1, 1, 10000.00, NULL),
(14, 10, 2, 2, 2, 10000.00, NULL),
(15, 10, 3, 3, 1, 8000.00, NULL),
(16, 11, 1, 1, 1, 10000.00, NULL),
(17, 12, 4, 5, 3, 8000.00, 'pedas'),
(18, 13, 5, 9, 2, 8000.00, 'extra pedas'),
(19, 13, 2, 1, 1, 10000.00, NULL),
(20, 14, 1, 3, 1, 10000.00, NULL),
(21, 14, 3, 2, 1, 8000.00, NULL),
(22, 15, 2, 6, 4, 10000.00, NULL),
(23, 16, 4, 7, 2, 8000.00, NULL),
(24, 16, 3, 1, 2, 8000.00, NULL),
(25, 17, 1, 4, 3, 10000.00, NULL),
(26, 18, 5, 5, 2, 8000.00, NULL),
(27, 18, 4, 1, 2, 8000.00, NULL),
(28, 19, 2, 8, 1, 10000.00, NULL),
(29, 20, 1, NULL, 9, 10000.00, NULL),
(30, 20, 5, 1, 1, 8000.00, NULL),
(31, 20, 3, 1, 1, 8000.00, NULL),
(32, 20, 1, 1, 1, 10000.00, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `varian_rasa`
--

CREATE TABLE `varian_rasa` (
  `id_rasa` int NOT NULL,
  `nama_rasa` varchar(50) NOT NULL,
  `keterangan` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

--
-- Dumping data for table `varian_rasa`
--

INSERT INTO `varian_rasa` (`id_rasa`, `nama_rasa`, `keterangan`) VALUES
(1, 'Original', 'Rasa asli tanpa bumbu tambahan'),
(2, 'Keju', 'Bubuk keju'),
(3, 'BBQ', 'Bumbu barbeque'),
(4, 'Blackpaper', 'Bumbu lada hitam'),
(5, 'Balado', 'Bumbu balado pedas'),
(6, 'Kari India', 'Bumbu kari india'),
(7, 'Parsley', 'Taburan parsley'),
(8, 'Daun Jeruk', 'Aroma daun jeruk'),
(9, 'Cabai', 'Bubuk cabai pedas');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `kasir`
--
ALTER TABLE `kasir`
  ADD PRIMARY KEY (`id_kasir`),
  ADD UNIQUE KEY `uid_kartu` (`uid_kartu`);

--
-- Indexes for table `kategori_menu`
--
ALTER TABLE `kategori_menu`
  ADD PRIMARY KEY (`id_kategori`);

--
-- Indexes for table `log_kasir`
--
ALTER TABLE `log_kasir`
  ADD PRIMARY KEY (`id_log`),
  ADD KEY `id_kasir` (`id_kasir`);

--
-- Indexes for table `menu`
--
ALTER TABLE `menu`
  ADD PRIMARY KEY (`id_menu`),
  ADD KEY `id_kategori` (`id_kategori`);

--
-- Indexes for table `menu_varian`
--
ALTER TABLE `menu_varian`
  ADD PRIMARY KEY (`id_menu`,`id_rasa`),
  ADD KEY `id_rasa` (`id_rasa`);

--
-- Indexes for table `pembayaran`
--
ALTER TABLE `pembayaran`
  ADD PRIMARY KEY (`id_pembayaran`),
  ADD KEY `id_transaksi` (`id_transaksi`);

--
-- Indexes for table `transaksi`
--
ALTER TABLE `transaksi`
  ADD PRIMARY KEY (`id_transaksi`),
  ADD UNIQUE KEY `kode_transaksi` (`kode_transaksi`),
  ADD KEY `fk_transaksi_kasir` (`id_kasir`);

--
-- Indexes for table `transaksi_detail`
--
ALTER TABLE `transaksi_detail`
  ADD PRIMARY KEY (`id_detail`),
  ADD KEY `id_transaksi` (`id_transaksi`),
  ADD KEY `id_menu` (`id_menu`),
  ADD KEY `id_rasa` (`id_rasa`);

--
-- Indexes for table `varian_rasa`
--
ALTER TABLE `varian_rasa`
  ADD PRIMARY KEY (`id_rasa`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `kasir`
--
ALTER TABLE `kasir`
  MODIFY `id_kasir` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `kategori_menu`
--
ALTER TABLE `kategori_menu`
  MODIFY `id_kategori` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT for table `log_kasir`
--
ALTER TABLE `log_kasir`
  MODIFY `id_log` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=20;

--
-- AUTO_INCREMENT for table `menu`
--
ALTER TABLE `menu`
  MODIFY `id_menu` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6;

--
-- AUTO_INCREMENT for table `pembayaran`
--
ALTER TABLE `pembayaran`
  MODIFY `id_pembayaran` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=19;

--
-- AUTO_INCREMENT for table `transaksi`
--
ALTER TABLE `transaksi`
  MODIFY `id_transaksi` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=21;

--
-- AUTO_INCREMENT for table `transaksi_detail`
--
ALTER TABLE `transaksi_detail`
  MODIFY `id_detail` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=33;

--
-- AUTO_INCREMENT for table `varian_rasa`
--
ALTER TABLE `varian_rasa`
  MODIFY `id_rasa` int NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `log_kasir`
--
ALTER TABLE `log_kasir`
  ADD CONSTRAINT `log_kasir_ibfk_1` FOREIGN KEY (`id_kasir`) REFERENCES `kasir` (`id_kasir`);

--
-- Constraints for table `menu`
--
ALTER TABLE `menu`
  ADD CONSTRAINT `menu_ibfk_1` FOREIGN KEY (`id_kategori`) REFERENCES `kategori_menu` (`id_kategori`) ON DELETE SET NULL ON UPDATE CASCADE;

--
-- Constraints for table `menu_varian`
--
ALTER TABLE `menu_varian`
  ADD CONSTRAINT `menu_varian_ibfk_1` FOREIGN KEY (`id_menu`) REFERENCES `menu` (`id_menu`) ON DELETE CASCADE,
  ADD CONSTRAINT `menu_varian_ibfk_2` FOREIGN KEY (`id_rasa`) REFERENCES `varian_rasa` (`id_rasa`) ON DELETE CASCADE;

--
-- Constraints for table `pembayaran`
--
ALTER TABLE `pembayaran`
  ADD CONSTRAINT `pembayaran_ibfk_1` FOREIGN KEY (`id_transaksi`) REFERENCES `transaksi` (`id_transaksi`) ON DELETE CASCADE;

--
-- Constraints for table `transaksi`
--
ALTER TABLE `transaksi`
  ADD CONSTRAINT `fk_transaksi_kasir` FOREIGN KEY (`id_kasir`) REFERENCES `kasir` (`id_kasir`);

--
-- Constraints for table `transaksi_detail`
--
ALTER TABLE `transaksi_detail`
  ADD CONSTRAINT `transaksi_detail_ibfk_1` FOREIGN KEY (`id_transaksi`) REFERENCES `transaksi` (`id_transaksi`) ON DELETE CASCADE,
  ADD CONSTRAINT `transaksi_detail_ibfk_2` FOREIGN KEY (`id_menu`) REFERENCES `menu` (`id_menu`),
  ADD CONSTRAINT `transaksi_detail_ibfk_3` FOREIGN KEY (`id_rasa`) REFERENCES `varian_rasa` (`id_rasa`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
