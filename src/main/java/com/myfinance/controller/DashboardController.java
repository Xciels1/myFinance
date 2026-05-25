package com.myfinance.controller;

import com.myfinance.SceneManager;
import com.myfinance.SessionManager;
import com.myfinance.model.Anggaran;
import com.myfinance.model.Pemasukan;
import com.myfinance.model.Pengeluaran;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * DashboardController — Mengelola halaman utama/dashboard.
 *
 * Tanggung jawab:
 * 1. Tampilkan kartu ringkasan (Saldo, Pemasukan, Pengeluaran)
 * 2. Isi BarChart pengeluaran per kategori dengan filter bulan/tahun
 * 3. Cek dan tampilkan peringatan anggaran yang terlampaui
 */
public class DashboardController implements Initializable {

    // --- Kartu Ringkasan ---
    @FXML private Label lblSambutan;
    @FXML private Label lblSaldo;
    @FXML private Label lblPemasukan;
    @FXML private Label lblPengeluaran;

    // --- BarChart & Filter ---
    @FXML private BarChart<String, Number> barChart;
    @FXML private ComboBox<String>         cbBulan;
    @FXML private ComboBox<String>         cbTahun;

    // --- Area Alert Anggaran ---
    @FXML private VBox  vboxAlert;
    @FXML private Label lblTidakAdaAlert;

    // Nama bulan untuk ComboBox
    private static final String[] NAMA_BULAN = {
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    /**
     * initialize() dipanggil otomatis oleh JavaFX setelah FXML selesai dimuat.
     * Inisialisasi ComboBox lalu muat semua data.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        isiComboBoxFilter();
        muatSemuaData();
    }

    /** Isi ComboBox bulan dan tahun, lalu set nilai default ke bulan/tahun ini */
    private void isiComboBoxFilter() {
        // Isi pilihan bulan
        for (String bulan : NAMA_BULAN) {
            cbBulan.getItems().add(bulan);
        }

        // Isi pilihan tahun: 3 tahun ke belakang hingga tahun ini
        int tahunIni = LocalDate.now().getYear();
        for (int t = tahunIni - 2; t <= tahunIni; t++) {
            cbTahun.getItems().add(String.valueOf(t));
        }

        // Set default ke bulan dan tahun sekarang
        cbBulan.getSelectionModel().select(LocalDate.now().getMonthValue() - 1);
        cbTahun.getSelectionModel().select(String.valueOf(tahunIni));
    }

    /** Muat semua data: sambutan, ringkasan, chart, dan alert anggaran */
    private void muatSemuaData() {
        muatRingkasan();
        muatBarChart();
        muatAlertAnggaran();
    }

    // =========================================================
    // 1. KARTU RINGKASAN
    // =========================================================

    private void muatRingkasan() {
        try {
            int idUser = SessionManager.getInstance().getIdUserAktif();
            String username = SessionManager.getInstance().getUserAktif().getUsername();

            // Sambutan dengan nama user
            lblSambutan.setText("Selamat datang, " + username + "!");

            // Hitung total dari model
            double totalMasuk  = Pemasukan.hitungTotalPemasukan(idUser);
            double totalKeluar = Pengeluaran.hitungTotalPengeluaran(idUser);
            double saldo       = totalMasuk - totalKeluar;

            // Tampilkan ke label
            lblSaldo.setText(formatRupiah(saldo));
            lblPemasukan.setText(formatRupiah(totalMasuk));
            lblPengeluaran.setText(formatRupiah(totalKeluar));

        } catch (Exception e) {
            lblSambutan.setText("Gagal memuat data.");
            e.printStackTrace();
        }
    }

    // =========================================================
    // 2. BARCHART PENGELUARAN PER KATEGORI
    // =========================================================

    /** Dipanggil saat ComboBox filter berubah */
    @FXML
    private void handleFilterChart() {
        muatBarChart();
        muatAlertAnggaran(); // Alert juga ikut difilter per bulan/tahun
    }

    private void muatBarChart() {
        try {
            int idUser = SessionManager.getInstance().getIdUserAktif();
            int bulan  = cbBulan.getSelectionModel().getSelectedIndex() + 1; // index 0 = Januari
            int tahun  = Integer.parseInt(cbTahun.getSelectionModel().getSelectedItem());

            // Ambil data dari model: HashMap<namaKategori, totalNominal>
            HashMap<String, Double> dataChart =
                Pengeluaran.hitungPengeluaranPerKategori(idUser, bulan, tahun);

            // Buat satu series untuk BarChart
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Pengeluaran");

            // Masukkan setiap kategori sebagai satu batang (bar)
            for (Map.Entry<String, Double> entry : dataChart.entrySet()) {
                series.getData().add(
                    new XYChart.Data<>(entry.getKey(), entry.getValue())
                );
            }

            // Bersihkan chart lama lalu isi dengan data baru
            barChart.getData().clear();

            if (dataChart.isEmpty()) {
                // Tidak ada data untuk bulan/tahun ini
                barChart.setTitle("Tidak ada pengeluaran pada periode ini.");
            } else {
                barChart.setTitle("");
                barChart.getData().add(series);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // 3. ALERT PERINGATAN ANGGARAN
    // =========================================================

    private void muatAlertAnggaran() {
        try {
            int idUser = SessionManager.getInstance().getIdUserAktif();
            int bulan  = cbBulan.getSelectionModel().getSelectedIndex() + 1;
            int tahun  = Integer.parseInt(cbTahun.getSelectionModel().getSelectedItem());

            // Ambil semua anggaran beserta realisasi pengeluaran bulan ini
            ArrayList<Anggaran> daftarAnggaran =
                Anggaran.getAllAnggaranDenganRealisasi(idUser, bulan, tahun);

            // Hapus semua label alert lama (kecuali label "tidak ada alert")
            vboxAlert.getChildren().removeIf(
                node -> node instanceof Label && node != lblTidakAdaAlert
            );

            // Cek satu per satu anggaran
            boolean adaYangMelebihi = false;
            for (Anggaran ang : daftarAnggaran) {
                if (ang.cekStatusAnggaran()) {
                    adaYangMelebihi = true;

                    // Buat label peringatan untuk kategori yang over budget
                    Label lblAlert = new Label(
                        "⚠  " + ang.getNamaKategori()
                        + " — Pengeluaran: " + formatRupiah(ang.getTotalPengeluaran())
                        + " / Limit: "       + formatRupiah(ang.getLimitNominal())
                        + "  (" + formatRupiah(ang.getTotalPengeluaran() - ang.getLimitNominal())
                        + " melebihi batas!)"
                    );
                    lblAlert.getStyleClass().add("alert-text");
                    lblAlert.setWrapText(true);

                    vboxAlert.getChildren().add(lblAlert);
                }
            }

            // Tampilkan atau sembunyikan pesan "semua aman"
            lblTidakAdaAlert.setVisible(!adaYangMelebihi);
            lblTidakAdaAlert.setManaged(!adaYangMelebihi);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // =========================================================
    // NAVIGASI SIDEBAR
    // =========================================================

    @FXML private void handleDashboard()         { muatSemuaData(); }
    @FXML private void handleTambah()            { SceneManager.getInstance().tampilkan(SceneManager.Halaman.TAMBAH); }
    @FXML private void handleRiwayat()           { SceneManager.getInstance().tampilkan(SceneManager.Halaman.RIWAYAT); }
    @FXML private void handleStatistik()         { SceneManager.getInstance().tampilkan(SceneManager.Halaman.STATISTIK); }
    @FXML private void handleAnggaran()          { SceneManager.getInstance().tampilkan(SceneManager.Halaman.ANGGARAN); }
    @FXML private void handleKategori()          { SceneManager.getInstance().tampilkan(SceneManager.Halaman.KATEGORI); }

    /** Tombol aksi cepat: langsung buka halaman Tambah */
    @FXML private void handleTambahPemasukan()   { SceneManager.getInstance().tampilkan(SceneManager.Halaman.TAMBAH); }
    @FXML private void handleTambahPengeluaran() { SceneManager.getInstance().tampilkan(SceneManager.Halaman.TAMBAH); }

    @FXML
    private void handleLogout() {
        // Hapus sesi lalu kembali ke Login
        SessionManager.getInstance().logout();
        SceneManager.getInstance().tampilkan(SceneManager.Halaman.LOGIN);
    }

    // =========================================================
    // HELPER
    // =========================================================

    /** Format angka ke format Rupiah: Rp 1.500.000 */
    private String formatRupiah(double nominal) {
        return "Rp " + String.format("%,.0f", nominal);
    }
}
