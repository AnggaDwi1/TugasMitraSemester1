-- Jalankan sekali setelah database mitrasemester1 dibuat dan dump lama diimpor.
-- Akun bernama Yuyut dijadikan OWNER; akun lain tetap KARYAWAN.
ALTER TABLE kasir
    ADD COLUMN role ENUM('OWNER', 'KARYAWAN') NOT NULL DEFAULT 'KARYAWAN',
    ADD COLUMN pin_hash VARCHAR(255) NULL,
    MODIFY COLUMN uid_kartu VARCHAR(30) NULL;

UPDATE kasir
SET role = CASE
    WHEN LOWER(TRIM(nama_kasir)) = 'yuyut' THEN 'OWNER'
    ELSE 'KARYAWAN'
END;

-- Dump awal hanya berisi Pak Owo. Buat akun Owner PIN-only jika Yuyut belum ada.
INSERT INTO kasir (nama_kasir, uid_kartu, status, role)
SELECT 'Yuyut', NULL, 'aktif', 'OWNER'
WHERE NOT EXISTS (
    SELECT 1 FROM kasir WHERE LOWER(TRIM(nama_kasir)) = 'yuyut'
);

-- Pastikan Owner yang sudah ada tetap mempunyai role Owner.
UPDATE kasir SET role = 'OWNER'
WHERE LOWER(TRIM(nama_kasir)) = 'yuyut';
