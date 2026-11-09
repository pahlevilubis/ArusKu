# ArusKu — Uangmu Mengalir ke Mana

Dibuat oleh **Pahlevi Lubis**.

Aplikasi pencatat pengeluaran untuk Android (dibuat untuk Samsung Galaxy S21 Ultra, jalan juga di Android 10 ke atas).
Transaksi BCA **tercatat sendiri dari notifikasi**, jadi hampir tidak perlu mengetik apa-apa. Semua data tersimpan
di HP saja (offline), tidak ada yang dikirim ke internet.

## Cara hitungnya

- **Patokan: uang yang keluar dari BCA = pengeluaran.** Top up GoPay dan tarik tunai langsung dihitung pengeluaran
  saat uangnya keluar dari BCA.
- **Transfer ke rekening sendiri** (HSBC / bank lain) **tidak dihitung**. Setiap ada transfer, ArusKu bertanya lewat
  notifikasi: *Rekening sendiri* atau *Bayar orang* — cukup sekali ketuk.
- **Uang masuk ke BCA** (isi dari rekening sendiri / pemasukan lain) tercatat sebagai "Diisi".
- **Uang tunai & GoPay** hanya **rincian** ("habisnya buat apa") dan tidak menambah total, supaya tidak dobel.

## Fitur

- **Catat otomatis** dari notifikasi *Financial Diary* BCA mobile / myBCA (nominal, kategori, jam)
- **Notif kecil yang bisa dibalas** untuk menambah catatan, misal "shopee sepatu" (bisa dimatikan)
- **Widget layar utama**: lihat sisa uang + tombol cepat *Ketik / Parkir / Bensin / Makan / Kopi* untuk uang tunai
- **Ketik singkat**: "bakso 25rb", "parkir 5", "bensin 50k" — nominal & kategori terisi sendiri,
  dan pilihan kategorimu diingat untuk berikutnya
- **Beranda**: perkiraan saldo, diisi vs terpakai, dan perkiraan *"sisa uangmu habis sekitar tanggal …"*
- **Rekap bulanan**: per kategori (naik/turun dibanding bulan lalu), **bocor halus** (transaksi kecil yang sering),
  **pengeluaran berulang** (langganan/tagihan), pola per minggu, per jam, hari kerja vs akhir pekan,
  dan catatan otomatis
- **Budget per kategori** dengan peringatan di 80% dan 100%
- **Ekspor CSV** untuk cadangan / dibuka di Excel
- **Log notifikasi bank** untuk menyiapkan pembaca baru (misal GoPay). Kode OTP/PIN tidak pernah disimpan.

## Download APK

Setiap kali ada perubahan, GitHub otomatis membuat APK baru (±5 menit).
Buka repo ini di HP → **Releases** (atau tab **Actions**) → download **ArusKu.apk**.

## Install di HP Samsung

1. Buka file **ArusKu.apk** lewat **My Files / File Saya**.
2. Kalau diminta, izinkan **Install unknown apps**, dan kalau muncul Play Protect pilih **Tetap instal**.
3. Buka **ArusKu** → izinkan notifikasi.
4. Di **Pengaturan → Baca notifikasi BCA** ketuk **Buka akses notifikasi**, cari ArusKu, aktifkan.
   - Kalau tombolnya abu-abu / muncul **"Setelan dibatasi"**: ketuk **Info aplikasi** → **⋮** (kanan atas) →
     **Izinkan setelan terbatas**, lalu ulangi.
5. Supaya tetap jalan di background: **Info aplikasi → Baterai → Tidak dibatasi**.
6. Isi **saldo BCA** sekali di Beranda supaya angka sisa sama dengan saldo asli.
7. Tambahkan widget: tekan lama layar utama → **Widget** → **ArusKu**.

Update versi baru bisa langsung ditimpa — data tidak hilang (tanda tangan aplikasi tetap, file `app/arusku.keystore`).

## Struktur kode (Kotlin + Jetpack Compose)

```
app/src/main/java/com/arusku/app/
├── MainActivity.kt          Layar utama
├── QuickAddActivity.kt      Catat cepat dari widget
├── data/
│   ├── Repo.kt              Database & aturan hitung
│   ├── Analysis.kt          Rekap bulanan, bocor halus, berulang, pola waktu
│   ├── Money.kt             Format Rupiah & pengurai "bakso 25rb"
│   └── Categorizer.kt       Tebak kategori dari kata
├── notif/
│   ├── BankListener.kt      Pembaca notifikasi
│   ├── BankParser.kt        Pengurai notifikasi BCA Financial Diary
│   └── Notifier.kt          Notifikasi ArusKu (konfirmasi, budget)
├── widget/ArusWidget.kt     Widget layar utama
└── ui/                      Beranda, Riwayat, Rekap, Pengaturan
```
