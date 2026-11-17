# Uji Midtrans Sandbox

> Ini integrasi development untuk simulasi. Backend lokal hanya terikat ke `127.0.0.1` dan menggunakan API sandbox. Jangan gunakan untuk pembayaran pelanggan atau mode produksi. Untuk produksi, backend harus dipasang pada server HTTPS dan menerima notifikasi Midtrans.

## Yang perlu disiapkan

1. Buat akun Midtrans dan ambil **Sandbox Server Key** dari menu *Settings → Access Keys*. Jangan kirim atau commit Server Key.
2. Pastikan database yang dipakai aplikasi sudah dipilih di phpMyAdmin, lalu jalankan `database/migrations/004_midtrans.sql` satu kali.
3. JDK 11 atau lebih baru dan Maven harus tersedia.

## Menjalankan backend lokal

Buka PowerShell di folder project. Jalankan blok berikut; masukkan Sandbox Server Key pada prompt tersembunyi. Terminal ini harus tetap terbuka selama pengujian:

```powershell
$secret = Read-Host "Sandbox Server Key" -AsSecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
try {
    $env:MIDTRANS_SERVER_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    & "C:\netbeans\java\maven\bin\mvn.cmd" -Dexec.mainClass=com.mycompany.mavenproject1.MidtransServer compile org.codehaus.mojo:exec-maven-plugin:3.5.1:java
} finally {
    Remove-Item Env:\MIDTRANS_SERVER_KEY -ErrorAction SilentlyContinue
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    $secret.Dispose()
}
```

Perintah memakai Maven pada `C:\netbeans\java\maven\bin\mvn.cmd`, sesuai lokasi NetBeans di komputer development ini. Jika letak Maven berbeda, ganti path tersebut. Kunci hanya dimasukkan pada prompt tersembunyi dan dihapus dari environment setelah backend berhenti.

## Uji dari aplikasi

1. Biarkan jendela PowerShell backend tetap berjalan.
2. Jalankan aplikasi kasir dari NetBeans seperti biasa.
3. Pilih menu, lalu pilih metode **Midtrans** dan tekan **Bayar**.
4. Halaman checkout Sandbox terbuka di browser. Gunakan data pembayaran uji yang tercantum pada [panduan resmi pengujian Sandbox](https://docs.midtrans.com/docs/testing-payment-on-sandbox).
5. Selama aplikasi kasir tetap terbuka, status akan diperiksa otomatis. Stok ditahan saat order dibuat; jika transaksi gagal atau kedaluwarsa, stok dikembalikan. Pembayaran baru dicatat dan transaksi menjadi selesai setelah status sukses terverifikasi.

## Batasan

Backend development ini memeriksa status dengan API Midtrans secara berkala, termasuk transaksi pending yang tersimpan saat backend dijalankan kembali. Aplikasi kasir perlu tetap terbuka untuk menampilkan pesan hasil dan struk otomatis. Sebelum produksi, pindahkan backend ke server HTTPS, gunakan Server Key produksi hanya di server, dan tambahkan endpoint notifikasi/webhook yang memverifikasi signature Midtrans.
