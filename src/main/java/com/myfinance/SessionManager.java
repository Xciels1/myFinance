package com.myfinance;

import com.myfinance.model.User;

/**
 * Class SessionManager - Menyimpan data pengguna yang sedang login.
 *
 * Menggunakan pola Singleton agar data user bisa diakses
 * dari Controller mana pun tanpa perlu passing objek secara manual.
 *
 * Tidak ada kode JavaFX di sini — murni Java.
 */
public class SessionManager {

    private static SessionManager instance;

    // User yang sedang aktif login
    private User userAktif;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    /** Simpan user setelah login berhasil */
    public void setUserAktif(User user) {
        this.userAktif = user;
    }

    /** Ambil user yang sedang login */
    public User getUserAktif() {
        return userAktif;
    }

    /** Ambil ID user aktif (shortcut yang sering dipakai Controller) */
    public int getIdUserAktif() {
        return userAktif != null ? userAktif.getIdUser() : -1;
    }

    /** Hapus sesi saat logout */
    public void logout() {
        this.userAktif = null;
    }

    /** Cek apakah ada user yang sedang login */
    public boolean isLoggedIn() {
        return userAktif != null;
    }
}
