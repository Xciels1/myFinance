package com.myfinance.controller;

import com.myfinance.SceneManager;
import com.myfinance.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

/**
 * RegisterController — Menangani logika halaman Registrasi.
 */
public class RegisterController {

    @FXML private TextField     tfUsername;
    @FXML private PasswordField pfPassword;
    @FXML private PasswordField pfKonfirmasi;
    @FXML private Label         lblPesan;

    /** Dipanggil saat tombol "Daftar" ditekan */
    @FXML
    private void handleRegister() {
        String username    = tfUsername.getText().trim();
        String password    = pfPassword.getText().trim();
        String konfirmasi  = pfKonfirmasi.getText().trim();

        // --- Validasi input ---
        if (username.isEmpty() || password.isEmpty() || konfirmasi.isEmpty()) {
            tampilkanPesan("Semua field wajib diisi.", false);
            return;
        }

        if (username.length() < 3) {
            tampilkanPesan("Username minimal 3 karakter.", false);
            return;
        }

        if (password.length() < 4) {
            tampilkanPesan("Password minimal 4 karakter.", false);
            return;
        }

        if (!password.equals(konfirmasi)) {
            tampilkanPesan("Password dan konfirmasi tidak sama.", false);
            return;
        }

        try {
            boolean berhasil = User.register(username, password);

            if (berhasil) {
                tampilkanPesan("Akun berhasil dibuat! Silakan login.", true);
                // Bersihkan field
                tfUsername.clear();
                pfPassword.clear();
                pfKonfirmasi.clear();
            } else {
                tampilkanPesan("Username sudah digunakan, coba yang lain.", false);
            }

        } catch (Exception e) {
            tampilkanPesan("Terjadi kesalahan: " + e.getMessage(), false);
            e.printStackTrace();
        }
    }

    /** Kembali ke halaman Login */
    @FXML
    private void handleKeLogin() {
        SceneManager.getInstance().tampilkan(SceneManager.Halaman.LOGIN);
    }

    /**
     * Tampilkan pesan dengan warna berbeda.
     * @param pesan   Teks pesan
     * @param sukses  true = hijau (berhasil), false = merah (error)
     */
    private void tampilkanPesan(String pesan, boolean sukses) {
        lblPesan.setText(sukses ? "✓ " + pesan : "⚠ " + pesan);
        lblPesan.getStyleClass().removeAll("label-expense", "label-income");
        lblPesan.getStyleClass().add(sukses ? "label-income" : "label-expense");
        lblPesan.setVisible(true);
    }
}
