package com.myfinance.controller;

import com.myfinance.SceneManager;
import com.myfinance.SessionManager;
import com.myfinance.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * LoginController — Menangani logika halaman Login.
 * Hanya berisi kode JavaFX UI; logika DB ada di model (User.java).
 */
public class LoginController {

    @FXML private TextField     tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private Label         lblError;

    /**
     * Dipanggil saat tombol "Login" ditekan.
     * Validasi input → panggil User.login() → navigasi ke Dashboard jika berhasil.
     */
    @FXML
    private void handleLogin() {
        String username = tfUsername.getText().trim();
        String password = pfPassword.getText().trim();

        // Validasi input tidak boleh kosong
        if (username.isEmpty() || password.isEmpty()) {
            tampilkanError("Username dan password tidak boleh kosong.");
            return;
        }

        try {
            // Coba login via model
            User user = User.login(username, password);

            if (user != null) {
                // Login berhasil: simpan user ke sesi global
                SessionManager.getInstance().setUserAktif(user);
                // Pindah ke halaman Dashboard
                SceneManager.getInstance().tampilkan(SceneManager.Halaman.DASHBOARD);
            } else {
                tampilkanError("Username atau password salah.");
            }

        } catch (Exception e) {
            tampilkanError("Terjadi kesalahan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Dipanggil saat klik link "Daftar di sini" */
    @FXML
    private void handleKeRegister() {
        SceneManager.getInstance().tampilkan(SceneManager.Halaman.REGISTER);
    }

    /** Tampilkan pesan error di label */
    private void tampilkanError(String pesan) {
        lblError.setText("⚠ " + pesan);
        lblError.setVisible(true);
    }
}
