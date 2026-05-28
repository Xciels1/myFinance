package com.myfinance.controller;

import com.myfinance.SceneManager;
import com.myfinance.SessionManager;
import com.myfinance.model.Anggaran;
import com.myfinance.model.Kategori;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * AnggaranController — Mengelola halaman Manajemen Anggaran.
 *
 * Fitur:
 * - Tambah anggaran baru (set limit per kategori)
 * - Edit anggaran yang sudah ada (klik tombol Edit di baris)
 * - Hapus anggaran
 * - Tabel menampilkan realisasi + status (Aman/Melebihi)
 *
 * FIX: Ditambahkan popup konfirmasi sebelum menyimpan anggaran.
 */
public class AnggaranController implements Initializable {

    // ── Form ──
    @FXML private Label              lblFormJudul;
    @FXML private ComboBox<Kategori> cbKategori;
    @FXML private TextField          tfLimit;
    @FXML private Label              lblFormError;
    @FXML private Button             btnSimpan;

    // ── Tabel ──
    @FXML private TableView<Anggaran>              tableAnggaran;
    @FXML private TableColumn<Anggaran, String>    kolKategori;
    @FXML private TableColumn<Anggaran, String>    kolLimit;
    @FXML private TableColumn<Anggaran, String>    kolRealisasi;
    @FXML private TableColumn<Anggaran, String>    kolSisa;
    @FXML private TableColumn<Anggaran, String>    kolStatus;
    @FXML private TableColumn<Anggaran, Void>      kolAksi;

    // ID anggaran yang sedang diedit (0 = mode tambah)
    private int idAnggaranEdit = 0;

    private final int bulanIni = LocalDate.now().getMonthValue();
    private final int tahunIni = LocalDate.now().getYear();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        muatKategoriComboBox();
        setupKolomTabel();
        setupCellFactoryAksi();
        muatData();
    }

    // =========================================================
    // SETUP
    // =========================================================

    /** Isi ComboBox hanya dengan kategori bertipe Pengeluaran */
    private void muatKategoriComboBox() {
        try {
            ArrayList<Kategori> list = Kategori.getKategoriByTipe("Pengeluaran");
            cbKategori.getItems().addAll(list);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void setupKolomTabel() {
        kolKategori.setCellValueFactory(
            d -> new SimpleStringProperty(d.getValue().getNamaKategori()));

        kolLimit.setCellValueFactory(
            d -> new SimpleStringProperty(formatRupiah(d.getValue().getLimitNominal())));

        kolRealisasi.setCellValueFactory(
            d -> new SimpleStringProperty(formatRupiah(d.getValue().getTotalPengeluaran())));

        kolSisa.setCellValueFactory(d -> {
            double sisa = d.getValue().hitungSisaAnggaran();
            return new SimpleStringProperty(formatRupiah(sisa));
        });

        // Kolom status: warna merah jika melebihi limit
        kolStatus.setCellValueFactory(d -> {
            boolean over = d.getValue().cekStatusAnggaran();
            return new SimpleStringProperty(over ? "⚠ Melebihi" : "✓ Aman");
        });
        kolStatus.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) { setText(null); setStyle(""); return; }
                setText(status);
                setStyle(status.contains("Melebihi")
                    ? "-fx-text-fill: #ef476f; -fx-font-weight: bold;"
                    : "-fx-text-fill: #06d6a0; -fx-font-weight: bold;");
            }
        });
    }

    private void setupCellFactoryAksi() {
        kolAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = new Button("✏ Edit");
            private final Button btnHapus = new Button("🗑");
            private final HBox   hbox     = new HBox(6, btnEdit, btnHapus);
            {
                btnEdit.getStyleClass().add("btn-warning");
                btnHapus.getStyleClass().add("btn-danger");

                btnEdit.setOnAction(e -> {
                    Anggaran ang = getTableView().getItems().get(getIndex());
                    isiFormUntukEdit(ang);
                });
                btnHapus.setOnAction(e -> {
                    Anggaran ang = getTableView().getItems().get(getIndex());
                    handleHapus(ang);
                });
            }
            @Override
            protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : hbox);
            }
        });
    }

    // =========================================================
    // FORM TAMBAH / EDIT
    // =========================================================

    /** Pre-fill form dengan data anggaran yang akan diedit */
    private void isiFormUntukEdit(Anggaran ang) {
        idAnggaranEdit = ang.getIdAnggaran();
        lblFormJudul.setText("Edit Anggaran");
        btnSimpan.setText("💾 Update");

        for (Kategori k : cbKategori.getItems()) {
            if (k.getIdKategori() == ang.getIdKategori()) {
                cbKategori.setValue(k);
                break;
            }
        }
        tfLimit.setText(String.valueOf((int) ang.getLimitNominal()));
    }

    @FXML
    private void handleSimpan() {
        // Validasi input
        if (cbKategori.getValue() == null) {
            tampilkanError("Pilih kategori terlebih dahulu.");
            return;
        }
        String inputLimit = tfLimit.getText().trim();
        if (inputLimit.isEmpty()) {
            tampilkanError("Limit tidak boleh kosong.");
            return;
        }
        double limit;
        try {
            limit = Double.parseDouble(inputLimit);
            if (limit <= 0) { tampilkanError("Limit harus lebih dari 0."); return; }
        } catch (NumberFormatException e) {
            tampilkanError("Limit harus berupa angka.");
            return;
        }

        String namaKategori = cbKategori.getValue().getNamaKategori();
        String labelAksi    = (idAnggaranEdit > 0) ? "memperbarui" : "menambahkan";

        // ── POPUP KONFIRMASI ──
        String pesanKonfirmasi = String.format(
            "Apakah Anda yakin ingin %s anggaran berikut?\n\n" +
            "  Kategori  : %s\n" +
            "  Limit     : %s\n" +
            "  Periode   : Bulan ini (%d/%d)",
            labelAksi,
            namaKategori,
            formatRupiah(limit),
            bulanIni,
            tahunIni
        );

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Anggaran");
        konfirmasi.setHeaderText("Konfirmasi " + (idAnggaranEdit > 0 ? "Edit" : "Tambah") + " Anggaran");
        konfirmasi.setContentText(pesanKonfirmasi);

        ButtonType btnYa    = new ButtonType("Ya, Simpan", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnBatal = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        konfirmasi.getButtonTypes().setAll(btnYa, btnBatal);

        Optional<ButtonType> hasil = konfirmasi.showAndWait();
        if (hasil.isEmpty() || hasil.get() != btnYa) {
            return; // User membatalkan
        }

        // ── PROSES SIMPAN ──
        int idUser     = SessionManager.getInstance().getIdUserAktif();
        int idKategori = cbKategori.getValue().getIdKategori();

        try {
            boolean berhasil = Anggaran.setLimit(idUser, idKategori, limit);
            if (berhasil) {
                handleBatalEdit(); // reset form
                muatData();
            } else {
                tampilkanError("Gagal menyimpan anggaran.");
            }
        } catch (Exception e) {
            tampilkanError("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Reset form ke mode tambah */
    @FXML
    private void handleBatalEdit() {
        idAnggaranEdit = 0;
        lblFormJudul.setText("Tambah Anggaran");
        btnSimpan.setText("💾 Simpan");
        cbKategori.setValue(null);
        tfLimit.clear();
        lblFormError.setText("");
    }

    private void handleHapus(Anggaran ang) {
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Hapus Anggaran");
        konfirmasi.setContentText(
            "Hapus anggaran untuk kategori \"" + ang.getNamaKategori() + "\"?");
        Optional<ButtonType> hasil = konfirmasi.showAndWait();
        if (hasil.isPresent() && hasil.get() == ButtonType.OK) {
            try {
                Anggaran.hapusAnggaran(ang.getIdAnggaran());
                muatData();
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    // =========================================================
    // MUAT DATA TABEL
    // =========================================================
    private void muatData() {
        try {
            int idUser = SessionManager.getInstance().getIdUserAktif();
            ArrayList<Anggaran> daftar =
                Anggaran.getAllAnggaranDenganRealisasi(idUser, bulanIni, tahunIni);
            tableAnggaran.setItems(FXCollections.observableArrayList(daftar));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void tampilkanError(String pesan) {
        lblFormError.setText("⚠ " + pesan);
    }

    private String formatRupiah(double n) {
        return "Rp " + String.format("%,.0f", n);
    }

    // ── Navigasi Sidebar ──
    @FXML private void handleDashboard()  { SceneManager.getInstance().tampilkan(SceneManager.Halaman.DASHBOARD); }
    @FXML private void handleTambah()    { SceneManager.getInstance().tampilkan(SceneManager.Halaman.TAMBAH); }
    @FXML private void handleRiwayat()   { SceneManager.getInstance().tampilkan(SceneManager.Halaman.RIWAYAT); }
    @FXML private void handleStatistik() { SceneManager.getInstance().tampilkan(SceneManager.Halaman.STATISTIK); }
    @FXML private void handleAnggaran()  { muatData(); }
    @FXML private void handleKategori()  { SceneManager.getInstance().tampilkan(SceneManager.Halaman.KATEGORI); }
    @FXML private void handleLogout()    { SessionManager.getInstance().logout(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.LOGIN); }
}
