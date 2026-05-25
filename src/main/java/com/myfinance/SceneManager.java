package com.myfinance;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Class SceneManager - Mengelola perpindahan antar halaman (scene).
 *
 * Singleton: satu instance dipakai seluruh aplikasi.
 * Controller cukup panggil SceneManager.getInstance().tampilkan(Halaman.XXX)
 * untuk berpindah halaman — tidak perlu tahu detail FXMLLoader.
 */
public class SceneManager {

    // ===================== Enum Halaman =====================
    // Daftarkan semua halaman beserta path FXML-nya di sini
    public enum Halaman {
        LOGIN       ("/com/myfinance/view/login.fxml"),
        REGISTER    ("/com/myfinance/view/register.fxml"),
        DASHBOARD   ("/com/myfinance/view/dashboard.fxml"),
        TAMBAH      ("/com/myfinance/view/tambah-transaksi.fxml"),
        RIWAYAT     ("/com/myfinance/view/riwayat.fxml"),
        STATISTIK   ("/com/myfinance/view/statistik.fxml"),
        ANGGARAN    ("/com/myfinance/view/anggaran.fxml"),
        KATEGORI    ("/com/myfinance/view/kategori.fxml");

        private final String pathFxml;

        Halaman(String pathFxml) {
            this.pathFxml = pathFxml;
        }

        public String getPathFxml() {
            return pathFxml;
        }
    }

    // ===================== Singleton =====================
    private static SceneManager instance;
    private Stage primaryStage;

    // Ukuran jendela aplikasi
    private static final double LEBAR  = 1024;
    private static final double TINGGI = 680;

    private SceneManager() {}

    public static SceneManager getInstance() {
        if (instance == null) {
            instance = new SceneManager();
        }
        return instance;
    }

    /** Dipanggil sekali dari Main.java untuk menyimpan referensi stage utama */
    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    // ===================== Navigasi =====================

    /**
     * Tampilkan halaman berdasarkan enum Halaman.
     * Otomatis memuat FXML, menerapkan CSS, lalu mengganti scene di stage.
     *
     * @param halaman Halaman yang ingin ditampilkan
     */
    public void tampilkan(Halaman halaman) {
        try {
            // Muat file FXML dari resources
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(halaman.getPathFxml())
            );
            Parent root = loader.load();

            // Buat scene baru dengan ukuran standar
            Scene scene = new Scene(root, LEBAR, TINGGI);

            // Terapkan stylesheet CSS global
            String cssPath = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            // Tampilkan di stage utama
            primaryStage.setScene(scene);
            primaryStage.show();

        } catch (IOException e) {
            System.err.println("Gagal memuat halaman: " + halaman.getPathFxml());
            e.printStackTrace();
        }
    }

    /**
     * Overload: tampilkan dan kembalikan FXMLLoader-nya.
     * Berguna ketika Controller perlu mengakses Controller halaman tujuan
     * untuk mengirim data (misal: mode edit di TambahTransaksi).
     */
    public FXMLLoader tampilkanDanAmbilLoader(Halaman halaman) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(halaman.getPathFxml())
            );
            Parent root = loader.load();

            Scene scene = new Scene(root, LEBAR, TINGGI);
            String cssPath = getClass().getResource("/css/style.css").toExternalForm();
            scene.getStylesheets().add(cssPath);

            primaryStage.setScene(scene);
            primaryStage.show();

            return loader; // Controller halaman tujuan bisa diambil dari sini

        } catch (IOException e) {
            System.err.println("Gagal memuat halaman: " + halaman.getPathFxml());
            e.printStackTrace();
            return null;
        }
    }
}
