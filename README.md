# SmartBudget 💰

**SmartBudget** adalah aplikasi manajemen keuangan pribadi modern untuk Android yang dibangun menggunakan **Jetpack Compose**, **Kotlin Coroutines & Flow**, dan **Room Database**. Dirancang dengan antarmuka yang elegan, responsif, dan mendukung tampilan refresh rate tinggi (120Hz).

---

## ✨ Fitur Utama

- 📊 **Dashboard Keuangan Komprehensif**: Ringkasan saldo total, pemasukan, pengeluaran, serta grafik pergerakan kas.
- 💳 **Multi-Wallet Management**: Kelola beberapa rekening, e-wallet, atau uang tunai dengan pencatatan terpisah dan fitur transfer antar-dompet.
- 🎯 **Target & Impian (Savings Goal)**: Pantau progres tabungan untuk berbagai tujuan keuangan Anda.
- 📝 **Pencatatan Transaksi Cepat**: Kategori pemasukan dan pengeluaran fleksibel dengan pemformatan nominal otomatis.
- 📈 **Analitik & Laporan Mendalam**: Visualisasi pengeluaran dan pemasukan berdasarkan kategori dan rentang waktu.
- 🧮 **Kalkulator Dompet**: Simulasi dan perencanaan alokasi dana secara langsung.
- 🌓 **Tema Gelap & Terang**: Dukungan dark mode otomatis dan dinamis sesuai preferensi.
- ⚡ **Performa Halus**: Dioptimalkan untuk navigasi cepat dan tampilan 120Hz pada perangkat yang kompatibel.

---

## 🛠️ Tech Stack & Arsitektur

- **Bahasa**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Arsitektur**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Lokal Database**: [Room Database](https://developer.android.com/training/data-storage/room) dengan Flow reactive query
- **Navigasi**: Jetpack Navigation Compose
- **Asynchronous**: Kotlin Coroutines & StateFlow

---

## 🚀 Memulai (Getting Started)

### Prasyarat
- Android Studio Ladybug / Koala atau versi lebih baru
- JDK 11 atau JDK 17
- Android SDK (minSdk: 26, targetSdk: 36)

### Instalasi & Menjalankan
1. Clone repositori:
   ```bash
   git clone https://github.com/YuutoEi/SmartBudget.git
   cd SmartBudget
   ```
2. Buka project di **Android Studio**.
3. Sinkronkan Gradle (`Sync Project with Gradle Files`).
4. Jalankan aplikasi pada emulator atau perangkat fisik Android (Run `app`).

---

## 📄 Lisensi

Didistribusikan di bawah lisensi MIT. Lihat file `LICENSE` untuk informasi lebih lanjut.
