Sistem Antrian Rumah Sakit RSRJ - Kelompok 2
Aplikasi mobile Android untuk manajemen antrian pasien di rumah sakit dengan fitur multi-role (Pasien, Dokter, dan Admin).

Sistem Antrian Rumah Sakit RSRJ adalah aplikasi mobile berbasis Android yang dirancang untuk memudahkan proses pendaftaran dan manajemen antrian pasien di rumah sakit. Aplikasi ini menerapkan sistem role-based access control dengan 3 jenis pengguna: Pasien, Dokter, dan Admin.

Fitur Utama:
✅ Multi-role system (Pasien, Dokter, Admin)

✅ Booking appointment dengan pilihan dokter, spesialisasi, tanggal, dan jam

✅ Real-time queue management dengan status antrian (Waiting, Called, Completed)

✅ Receipt generator untuk download struk antrian dalam format PNG

✅ Notification system untuk reminder antrian

✅ Patient history tracking untuk riwayat kunjungan

✅ Doctor dashboard untuk manajemen antrian pasien

✅ Admin panel untuk manajemen dokter, pasien, dan jadwal

✅ News feed untuk berita kesehatan terkini

User Roles & Fitur
1. Pasien = user@example.com, password123
Login/Register dengan sistem autentikasi

Browse dokter berdasarkan spesialisasi

Booking appointment dengan memilih tanggal dan jam

Lihat nomor antrian real-time

Download struk antrian

Lihat riwayat kunjungan

Akses berita kesehatan

2. Dokter = dokter@rumahsakit.com, dokter123
Login dengan kredensial dokter

Dashboard dengan statistik harian:

Total pasien hari ini

Antrian aktif

Pasien selesai diperiksa

Kelola antrian pasien:

Panggil pasien (status: Waiting → Called)

Selesaikan pemeriksaan (status: Called → Completed)

Validasi: hanya 1 pasien yang bisa dipanggil dalam waktu bersamaan

Lihat riwayat pasien dengan diagnosis dan resep

3. Admin  = admin@rumahsakit.com, admin123
Dashboard admin dengan overview sistem

Manajemen dokter (tambah, edit, hapus)

Manajemen pasien

Manajemen jadwal dokter

View reports dan statistik

Pengaturan sistem

Tech Stack
Platform & Language:
Platform: Android (Kotlin)

IDE: Android Studio

Min SDK: Android 7.0 (API Level 24)

Target SDK: Android 14 (API Level 34)

Libraries & Dependencies:
UI: Material Design Components, BottomNavigationView, RecyclerView

Networking: Retrofit (untuk news API)

Image Loading: Glide

Storage: SharedPreferences (untuk session management)

Notifications: NotificationManager, AlarmManager

PDF/Image: Bitmap, Canvas (untuk receipt generation)

Architecture:
Pattern: Fragment-based navigation dengan Single Activity

Data: Object-based data source (in-memory storage)

Role Management: PreferencesHelper untuk session & role handling

