package com.myfinance.controller;

import com.myfinance.SceneManager;
import com.myfinance.SessionManager;
import com.myfinance.model.Kategori;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * KategoriController — CRUD lengkap untuk Kategori Transaksi.
 *
 * Fitur:
 * - Tambah kategori baru (Pemasukan / Pengeluaran)
 * - Edit nama dan tipe kategori via tombol di baris tabel
 * - Hapus kategori (dengan konfirmasi)
 */
public class KategoriController implements Initializable {

    // ── Form ──
    @FXML private Label              lblFormJudul;
    @FXML private TextField          tfNama;
    @FXML private ComboBox<String>   cbTipe;
    @FXML private Label              lblFormError;
    @FXML private Button             btnSimpan;

    // ── Tabel ──
    @FXML private TableView<Kategori>              tableKategori;
    @FXML private TableColumn<Kategori, String>    kolNama;
    @FXML private TableColumn<Kategori, String>    kolTipe;
    @FXML private TableColumn<Kategori, Void>      kolAksi;

    // ID kategori yang sedang diedit (0 = mode tambah)
    private int idKategoriEdit = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cbTipe.getItems().addAll("Pemasukan", "Pengeluaran");
        setupKolomTabel();
        setupCellFactoryAksi();
        muatData();
    }

    // =========================================================
    // SETUP TABEL
    // =========================================================
    private void setupKolomTabel() {
        kolNama.setCellValueFactory(
            d -> new SimpleStringProperty(d.getValue().getNamaKategori()));

        // Kolom tipe dengan warna berbeda
        kolTipe.setCellValueFactory(
            d -> new SimpleStringProperty(d.getValue().getTipe()));
        kolTipe.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String tipe, boolean empty) {
                super.updateItem(tipe, empty);
                if (empty || tipe == null) { setText(null); setStyle(""); return; }
                setText(tipe);
                setStyle(tipe.equals("Pemasukan")
                    ? "-fx-text-fill: #06d6a0; -fx-font-weight: bold;"
                    : "-fx-text-fill: #ef476f; -fx-font-weight: bold;");
            }
        });
    }

    private void setupCellFactoryAksi() {
        kolAksi.setCellFactory(col -> new TableCell<>() {
            private final Button btnEdit  = new Button("✏ Edit");
            private final Button btnHapus = new Button("🗑 Hapus");
            private final HBox   hbox     = new HBox(6, btnEdit, btnHapus);
            {
                btnEdit.getStyleClass().add("btn-warning");
                btnHapus.getStyleClass().add("btn-danger");

                btnEdit.setOnAction(e -> {
                    Kategori kat = getTableView().getItems().get(getIndex());
                    isiFormUntukEdit(kat);
                });
                btnHapus.setOnAction(e -> {
                    Kategori kat = getTableView().getItems().get(getIndex());
                    handleHapus(kat);
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
    private void isiFormUntukEdit(Kategori kat) {
        idKategoriEdit = kat.getIdKategori();
        lblFormJudul.setText("Edit Kategori");
        btnSimpan.setText("💾 Update");
        tfNama.setText(kat.getNamaKategori());
        cbTipe.setValue(kat.getTipe());
        lblFormError.setText("");
    }

    @FXML
    private void handleSimpan() {
        String nama = tfNama.getText().trim();
        String tipe = cbTipe.getValue();

        // Validasi
        if (nama.isEmpty()) { tampilkanError("Nama kategori tidak boleh kosong."); return; }
        if (nama.length() < 2) { tampilkanError("Nama minimal 2 karakter."); return; }
        if (tipe == null) { tampilkanError("Pilih tipe kategori."); return; }

        try {
            boolean berhasil;
            if (idKategoriEdit > 0) {
                // Mode edit: update data yang ada
                berhasil = Kategori.updateKategori(idKategoriEdit, nama, tipe);
            } else {
                // Mode tambah: buat kategori baru
                berhasil = Kategori.tambahKategori(nama, tipe);
            }

            if (berhasil) {
                handleBatalEdit(); // reset form
                muatData();        // refresh tabel
            } else {
                tampilkanError("Gagal menyimpan kategori.");
            }
        } catch (Exception e) {
            tampilkanError("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Reset form ke mode tambah */
    @FXML
    private void handleBatalEdit() {
        idKategoriEdit = 0;
        lblFormJudul.setText("Tambah Kategori");
        btnSimpan.setText("💾 Simpan");
        tfNama.clear();
        cbTipe.setValue(null);
        lblFormError.setText("");
    }

    private void handleHapus(Kategori kat) {
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Hapus Kategori");
        konfirmasi.setContentText(
            "Hapus kategori \"" + kat.getNamaKategori() + "\"?\n"
            + "Peringatan: gagal jika kategori masih dipakai oleh transaksi.");
        Optional<ButtonType> hasil = konfirmasi.showAndWait();
        if (hasil.isPresent() && hasil.get() == ButtonType.OK) {
            try {
                boolean berhasil = Kategori.hapusKategori(kat.getIdKategori());
                if (berhasil) {
                    muatData();
                } else {
                    new Alert(Alert.AlertType.WARNING,
                        "Kategori tidak bisa dihapus karena masih dipakai oleh transaksi.")
                        .showAndWait();
                }
            } catch (Exception e) {
                new Alert(Alert.AlertType.ERROR,
                    "Gagal menghapus: kategori mungkin masih digunakan oleh transaksi.")
                    .showAndWait();
            }
        }
    }

    // =========================================================
    // MUAT DATA TABEL
    // =========================================================
    private void muatData() {
        try {
            ArrayList<Kategori> daftar = Kategori.getAllKategori();
            tableKategori.setItems(FXCollections.observableArrayList(daftar));
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void tampilkanError(String pesan) { lblFormError.setText("⚠ " + pesan); }

    // ── Navigasi Sidebar ──
    @FXML private void handleDashboard()  { SceneManager.getInstance().tampilkan(SceneManager.Halaman.DASHBOARD); }
    @FXML private void handleTambah()    { SceneManager.getInstance().tampilkan(SceneManager.Halaman.TAMBAH); }
    @FXML private void handleRiwayat()   { SceneManager.getInstance().tampilkan(SceneManager.Halaman.RIWAYAT); }
    @FXML private void handleStatistik() { SceneManager.getInstance().tampilkan(SceneManager.Halaman.STATISTIK); }
    @FXML private void handleAnggaran()  { SceneManager.getInstance().tampilkan(SceneManager.Halaman.ANGGARAN); }
    @FXML private void handleKategori()  { muatData(); }
    @FXML private void handleLogout()    { SessionManager.getInstance().logout(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.LOGIN); }
}
