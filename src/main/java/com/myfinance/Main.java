package com.myfinance;

import com.myfinance.model.Database;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Main.java - Titik masuk (entry point) aplikasi JavaFX myFinance.
 *
 * Urutan inisialisasi:
 * 1. Inisialisasi tabel SQLite (buat jika belum ada)
 * 2. Daftarkan stage utama ke SceneManager
 * 3. Tampilkan halaman Login
 */
public class Main extends Application {

    @Override
    public void start(Stage primaryStage) {
        try {
            // 1. Inisialisasi database (buat semua tabel + data kategori awal)
            Database.getInstance().inisialisasiTabel();

            // 2. Atur judul dan ukuran minimum jendela
            primaryStage.setTitle("myFinance — Manajemen Keuangan Pribadi");
            primaryStage.setMinWidth(900);
            primaryStage.setMinHeight(600);
            primaryStage.setResizable(true);

            // 3. Daftarkan stage ke SceneManager agar bisa diakses dari mana saja
            SceneManager.getInstance().setPrimaryStage(primaryStage);

            // 4. Tampilkan halaman Login sebagai halaman awal
            SceneManager.getInstance().tampilkan(SceneManager.Halaman.LOGIN);

        } catch (Exception e) {
            System.err.println("Gagal menginisialisasi aplikasi: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void stop() {
        // Tutup koneksi database saat aplikasi ditutup
        try {
            Database.getInstance().disconnect();
            System.out.println("Koneksi database ditutup.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
