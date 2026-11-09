# Menyiapkan database lokal

Proyek ini menyertakan dump database di `src/main/java/mitrasemester1 (1).sql`.
Database teman tidak ikut tersalin saat proyek di-clone; setiap komputer perlu
MySQL sendiri atau terhubung ke server database yang memang bisa diakses bersama.

## Menyiapkan salinan lokal

1. Jalankan MySQL dari XAMPP.
2. Buka phpMyAdmin, buat database bernama `db_mitrasemester1`.
3. Pilih database itu, lalu impor file dump di atas.
4. Jalankan `database/migrations/001_roles_and_pin.sql` sekali pada database yang sama.
   Migrasi menetapkan akun `Yuyut` sebagai `OWNER`, membuat akun PIN-only bila belum
   ada, dan menjadikan akun lama selain Yuyut sebagai `KARYAWAN`.
5. Jalankan aplikasi. Pengaturan bawaan koneksi adalah `localhost:3306`, user `root`,
   password kosong (konfigurasi Laragon yang terdeteksi).

Jika MySQL memakai kredensial berbeda, atur properti Java `kasir.db.host`,
`kasir.db.port`, `kasir.db.user`, `kasir.db.password`, dan `kasir.db` saat menjalankan
aplikasi. Jangan simpan password database ke repositori.

## Menyiapkan PIN

Login RFID tetap tersedia untuk akun yang memiliki kartu setelah migrasi. Akun
Yuyut baru adalah PIN-only. Buka `PinUtil.java` di NetBeans lalu pilih **Run File**;
masukkan PIN minimal 4 digit pada dialog. Hash akan disalin ke clipboard. Kolom
database menyimpan hash PBKDF2, bukan PIN asli.

Contoh SQL setelah hash dihasilkan:

```sql
UPDATE kasir SET pin_hash = '<hash-yang-dihasilkan>' WHERE nama_kasir = 'Yuyut';
```

Jangan gunakan PIN contoh atau mengirim PIN melalui chat.
