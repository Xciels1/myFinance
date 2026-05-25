package com.myfinance.model;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Class User - Merepresentasikan data pengguna aplikasi.
 * Menangani registrasi, login, dan validasi password.
 *
 * Relasi: User ke Transaksi bersifat One-to-Many (1 user, banyak transaksi).
 */
public class User {

    // ===================== Atribut =====================
    private int    idUser;
    private String username;
    private String password;

    // ===================== Constructor =====================
    public User(int idUser, String username, String password) {
        this.idUser   = idUser;
        this.username = username;
        this.password = password;
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // ===================== Getters & Setters =====================
    public int    getIdUser()   { return idUser; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Validasi apakah password yang diinput cocok dengan yang tersimpan.
     * Catatan: Untuk kesederhanaan, password disimpan plaintext.
     * Di produksi nyata, gunakan hashing (BCrypt).
     */
    public boolean validasiPassword(String inputPassword) {
        return this.password.equals(inputPassword);
    }

    // ===================== Operasi Database (Static) =====================

    /**
     * Proses LOGIN: Mencari user berdasarkan username dan password di database.
     *
     * @return Objek User jika berhasil, null jika gagal
     */
    public static User login(String username, String password) throws SQLException {
        String sql = "SELECT * FROM user WHERE username = ? AND password = ?";
        ResultSet rs = Database.getInstance().executeQuery(sql, username, password);

        if (rs.next()) {
            // Login berhasil, buat dan kembalikan objek User
            return new User(
                rs.getInt("id_user"),
                rs.getString("username"),
                rs.getString("password")
            );
        }
        // Login gagal
        return null;
    }

    /**
     * Proses REGISTRASI: Menyimpan akun baru ke database.
     *
     * @return true jika berhasil, false jika username sudah dipakai
     */
    public static boolean register(String username, String password) throws SQLException {
        // Cek apakah username sudah ada
        String cekSql = "SELECT COUNT(*) FROM user WHERE username = ?";
        ResultSet rs   = Database.getInstance().executeQuery(cekSql, username);
        if (rs.next() && rs.getInt(1) > 0) {
            return false; // Username sudah dipakai
        }

        // Simpan akun baru
        String insertSql = "INSERT INTO user (username, password) VALUES (?, ?)";
        int rows = Database.getInstance().executeUpdate(insertSql, username, password);
        return rows > 0;
    }

    @Override
    public String toString() {
        return "User{id=" + idUser + ", username='" + username + "'}";
    }
}
