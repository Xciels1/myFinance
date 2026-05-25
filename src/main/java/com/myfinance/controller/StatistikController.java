package com.myfinance.controller;

import com.myfinance.SceneManager;
import com.myfinance.SessionManager;
import com.myfinance.model.Pemasukan;
import com.myfinance.model.Pengeluaran;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * StatistikController — Halaman visualisasi keuangan lengkap.
 * BarChart pengeluaran per kategori + ringkasan total periode.
 */
public class StatistikController implements Initializable {

    @FXML private ComboBox<String>         cbBulan;
    @FXML private ComboBox<String>         cbTahun;
    @FXML private Label                    lblPeriode;
    @FXML private BarChart<String, Number> barChart;
    @FXML private Label                    lblTotalMasuk;
    @FXML private Label                    lblTotalKeluar;
    @FXML private Label                    lblSelisih;

    private static final String[] NAMA_BULAN = {
        "Januari","Februari","Maret","April","Mei","Juni",
        "Juli","Agustus","September","Oktober","November","Desember"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        isiComboBox();
        muatData();
    }

    private void isiComboBox() {
        for (String b : NAMA_BULAN) cbBulan.getItems().add(b);
        int tahunIni = LocalDate.now().getYear();
        for (int t = tahunIni - 2; t <= tahunIni; t++)
            cbTahun.getItems().add(String.valueOf(t));

        // Default: bulan & tahun saat ini
        cbBulan.getSelectionModel().select(LocalDate.now().getMonthValue() - 1);
        cbTahun.setValue(String.valueOf(tahunIni));
    }

    @FXML
    private void handleFilter() {
        muatData();
    }

    private void muatData() {
        if (cbBulan.getValue() == null || cbTahun.getValue() == null) return;

        int idUser = SessionManager.getInstance().getIdUserAktif();
        int bulan  = cbBulan.getSelectionModel().getSelectedIndex() + 1;
        int tahun  = Integer.parseInt(cbTahun.getValue());

        lblPeriode.setText("— " + NAMA_BULAN[bulan - 1] + " " + tahun);

        muatBarChart(idUser, bulan, tahun);
        muatRingkasan(idUser, bulan, tahun);
    }

    // =========================================================
    // BARCHART
    // =========================================================
    private void muatBarChart(int idUser, int bulan, int tahun) {
        try {
            HashMap<String, Double> dataMap =
                Pengeluaran.hitungPengeluaranPerKategori(idUser, bulan, tahun);

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Pengeluaran");

            // Urutkan dari terbesar ke terkecil agar grafik lebih informatif
            dataMap.entrySet().stream()
                   .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                   .forEach(e -> series.getData().add(
                       new XYChart.Data<>(e.getKey(), e.getValue())));

            barChart.getData().clear();
            if (dataMap.isEmpty()) {
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
    // RINGKASAN PERIODE
    // =========================================================
    private void muatRingkasan(int idUser, int bulan, int tahun) {
        try {
            // Total pemasukan periode ini (filter bulan+tahun)
            // Kita pakai query dari TransaksiItem dengan filter
            double totalMasuk  = hitungTotalPeriode(idUser, "Pemasukan", bulan, tahun);
            double totalKeluar = hitungTotalPeriode(idUser, "Pengeluaran", bulan, tahun);
            double selisih     = totalMasuk - totalKeluar;

            lblTotalMasuk.setText(formatRupiah(totalMasuk));
            lblTotalKeluar.setText(formatRupiah(totalKeluar));
            lblSelisih.setText(formatRupiah(selisih));

            // Warna selisih: hijau jika hemat, merah jika defisit
            lblSelisih.getStyleClass().removeAll("label-income", "label-expense");
            lblSelisih.getStyleClass().add(selisih >= 0 ? "label-income" : "label-expense");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Hitung total transaksi (pemasukan atau pengeluaran) untuk periode tertentu.
     * Menggunakan TransaksiItem.getSemuaTransaksi() lalu jumlahkan nominalnya.
     */
    private double hitungTotalPeriode(int idUser, String tipe, int bulan, int tahun)
            throws Exception {
        var daftar = com.myfinance.model.TransaksiItem
                         .getSemuaTransaksi(idUser, tipe, bulan, tahun);
        return daftar.stream()
                     .mapToDouble(com.myfinance.model.TransaksiItem::getNominal)
                     .sum();
    }

    private String formatRupiah(double nominal) {
        return "Rp " + String.format("%,.0f", nominal);
    }

    // ── Navigasi Sidebar ──
    @FXML private void handleDashboard()  { SceneManager.getInstance().tampilkan(SceneManager.Halaman.DASHBOARD); }
    @FXML private void handleTambah()    { SceneManager.getInstance().tampilkan(SceneManager.Halaman.TAMBAH); }
    @FXML private void handleRiwayat()   { SceneManager.getInstance().tampilkan(SceneManager.Halaman.RIWAYAT); }
    @FXML private void handleStatistik() { muatData(); }
    @FXML private void handleAnggaran()  { SceneManager.getInstance().tampilkan(SceneManager.Halaman.ANGGARAN); }
    @FXML private void handleKategori()  { SceneManager.getInstance().tampilkan(SceneManager.Halaman.KATEGORI); }
    @FXML private void handleLogout()    { SessionManager.getInstance().logout(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.LOGIN); }
}
