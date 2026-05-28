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
 *
 * FIX: Scene di-reuse agar state fullscreen/maximize tidak hilang saat ganti halaman.
 */
public class SceneManager {

    // ===================== Enum Halaman =====================
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

    // Ukuran jendela awal (hanya dipakai saat scene pertama kali dibuat)
    private static final double LEBAR  = 1024;
    private static final double TINGGI = 680;

    // Scene tunggal yang di-reuse antar halaman
    private Scene mainScene;

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
     * Scene yang sama di-reuse agar ukuran/fullscreen stage tetap terjaga.
     */
    public void tampilkan(Halaman halaman) {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource(halaman.getPathFxml())
            );
            Parent root = loader.load();

            if (mainScene == null) {
                // Pertama kali: buat Scene dengan ukuran awal
                mainScene = new Scene(root, LEBAR, TINGGI);
                String cssPath = getClass().getResource("/css/style.css").toExternalForm();
                mainScene.getStylesheets().add(cssPath);
                primaryStage.setScene(mainScene);
            } else {
                // Selanjutnya: ganti root saja, Scene/Stage tidak diganti
                // sehingga state fullscreen / maximize tetap terjaga
                mainScene.setRoot(root);
            }

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

            if (mainScene == null) {
                mainScene = new Scene(root, LEBAR, TINGGI);
                String cssPath = getClass().getResource("/css/style.css").toExternalForm();
                mainScene.getStylesheets().add(cssPath);
                primaryStage.setScene(mainScene);
            } else {
                mainScene.setRoot(root);
            }

            primaryStage.show();
            return loader;

        } catch (IOException e) {
            System.err.println("Gagal memuat halaman: " + halaman.getPathFxml());
            e.printStackTrace();
            return null;
        }
    }
}
