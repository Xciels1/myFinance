package com.myfinance.model;

import java.sql.*;

/**
 * Class Database - Mengelola koneksi ke SQLite menggunakan pola Singleton.
 * Semua class lain bergantung (dependency) pada class ini untuk operasi data.
 */
public class Database {

    // Path file database SQLite (akan dibuat otomatis jika belum ada)
    private static final String URL = "jdbc:sqlite:myfinance.db";

    // Instance tunggal (Singleton)
    private static Database instance;

    // Objek koneksi JDBC
    private Connection connection;

    // Constructor private agar tidak bisa di-new dari luar
    private Database() {}

    /**
     * Mengambil instance Database (Singleton).
     * Jika belum ada, buat baru; jika sudah ada, kembalikan yang lama.
     */
    public static Database getInstance() {
        if (instance == null) {
            instance = new Database();
        }
        return instance;
    }

    /**
     * Membuka koneksi ke file SQLite.
     * Koneksi hanya dibuka jika belum ada atau sudah tertutup.
     */
    public Connection connect() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(URL);
        }
        return connection;
    }

    /**
     * Menutup koneksi database.
     */
    public void disconnect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    /**
     * Menjalankan query SELECT dan mengembalikan ResultSet.
     * Gunakan '?' sebagai placeholder untuk mencegah SQL Injection.
     *
     * @param sql    Query SQL dengan placeholder '?'
     * @param params Nilai-nilai pengganti placeholder secara berurutan
     */
    public ResultSet executeQuery(String sql, Object... params) throws SQLException {
        // closeOnCompletion() memastikan PreparedStatement ditutup
        // otomatis saat ResultSet-nya ditutup, mencegah resource leak.
        PreparedStatement ps = connect().prepareStatement(sql);
        ps.closeOnCompletion();
        // Isi setiap placeholder dengan nilai yang diberikan
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
        return ps.executeQuery();
    }

    /**
     * Menjalankan query INSERT, UPDATE, atau DELETE.
     *
     * @return Jumlah baris yang terpengaruh
     */
    public int executeUpdate(String sql, Object... params) throws SQLException {
        try (PreparedStatement ps = connect().prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            return ps.executeUpdate();
        }
    }

    /**
     * Inisialisasi semua tabel database saat aplikasi pertama kali dijalankan.
     * Menggunakan "CREATE TABLE IF NOT EXISTS" agar tidak error jika sudah ada.
     */
    public void inisialisasiTabel() throws SQLException {
        Statement stmt = connect().createStatement();

        // --- DDL Tabel user ---
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS user (
                id_user   INTEGER PRIMARY KEY AUTOINCREMENT,
                username  VARCHAR(50) UNIQUE NOT NULL,
                password  VARCHAR(255) NOT NULL
            )
            """);

        // --- DDL Tabel kategori ---
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS kategori (
                id_kategori    INTEGER PRIMARY KEY AUTOINCREMENT,
                nama_kategori  VARCHAR(50) NOT NULL,
                tipe           VARCHAR(20) NOT NULL CHECK(tipe IN ('Pemasukan','Pengeluaran'))
            )
            """);

        // --- DDL Tabel transaksi ---
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS transaksi (
                id_transaksi  INTEGER PRIMARY KEY AUTOINCREMENT,
                id_user       INTEGER NOT NULL,
                id_kategori   INTEGER NOT NULL,
                tanggal       DATE NOT NULL,
                nominal       DOUBLE NOT NULL,
                keterangan    TEXT,
                FOREIGN KEY (id_user)     REFERENCES user(id_user),
                FOREIGN KEY (id_kategori) REFERENCES kategori(id_kategori)
            )
            """);

        // --- DDL Tabel anggaran ---
        stmt.execute("""
            CREATE TABLE IF NOT EXISTS anggaran (
                id_anggaran    INTEGER PRIMARY KEY AUTOINCREMENT,
                id_user        INTEGER NOT NULL,
                id_kategori    INTEGER NOT NULL,
                limit_nominal  DOUBLE NOT NULL,
                FOREIGN KEY (id_user)     REFERENCES user(id_user),
                FOREIGN KEY (id_kategori) REFERENCES kategori(id_kategori),
                UNIQUE(id_user, id_kategori)
            )
            """);

        // Isi kategori default jika tabel masih kosong
        ResultSet rs = executeQuery("SELECT COUNT(*) FROM kategori");
        if (rs.next() && rs.getInt(1) == 0) {
            String insertKat = "INSERT INTO kategori (nama_kategori, tipe) VALUES (?, ?)";
            executeUpdate(insertKat, "Gaji",          "Pemasukan");
            executeUpdate(insertKat, "Freelance",     "Pemasukan");
            executeUpdate(insertKat, "Makan",         "Pengeluaran");
            executeUpdate(insertKat, "Transport",     "Pengeluaran");
            executeUpdate(insertKat, "Hiburan",       "Pengeluaran");
            executeUpdate(insertKat, "Belanja",       "Pengeluaran");
            executeUpdate(insertKat, "Lainnya",       "Pengeluaran");
        }

        stmt.close();
        System.out.println("Database berhasil diinisialisasi.");
    }
}
