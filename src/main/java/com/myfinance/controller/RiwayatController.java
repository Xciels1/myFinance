package com.myfinance.controller;

import com.myfinance.SceneManager;
import com.myfinance.SessionManager;
import com.myfinance.model.TransaksiItem;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * RiwayatController — Menampilkan semua transaksi dalam TableView.
 *
 * Fitur utama:
 * - Filter berdasarkan tipe, bulan, tahun
 * - CellFactory pada kolom Aksi untuk tombol Edit + Hapus di setiap baris
 * - Klik Edit → set static field di TambahTransaksiController → navigasi ke form
 */
public class RiwayatController implements Initializable {

    // ── Filter ──
    @FXML private ComboBox<String> cbFilterTipe;
    @FXML private ComboBox<String> cbFilterBulan;
    @FXML private ComboBox<String> cbFilterTahun;

    // ── TableView & Kolom ──
    @FXML private TableView<TransaksiItem>   tableRiwayat;
    @FXML private TableColumn<TransaksiItem, String> kolTanggal;
    @FXML private TableColumn<TransaksiItem, String> kolKategori;
    @FXML private TableColumn<TransaksiItem, String> kolTipe;
    @FXML private TableColumn<TransaksiItem, String> kolNominal;
    @FXML private TableColumn<TransaksiItem, String> kolKeterangan;
    @FXML private TableColumn<TransaksiItem, Void>   kolAksi;

    @FXML private Label lblJumlah;

    private static final String[] NAMA_BULAN = {
        "Januari","Februari","Maret","April","Mei","Juni",
        "Juli","Agustus","September","Oktober","November","Desember"
    };

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        isiComboBoxFilter();
        setupKolomTabel();
        setupCellFactoryAksi();   // pasang tombol Edit+Hapus di kolom Aksi
        muatData();
    }

    // =========================================================
    // SETUP COMBO BOX FILTER
    // =========================================================
    private void isiComboBoxFilter() {
        cbFilterTipe.getItems().addAll("Semua", "Pemasukan", "Pengeluaran");
        cbFilterTipe.setValue("Semua");

        cbFilterBulan.getItems().add("Semua Bulan");
        for (String b : NAMA_BULAN) cbFilterBulan.getItems().add(b);
        cbFilterBulan.setValue("Semua Bulan");

        int tahunIni = LocalDate.now().getYear();
        cbFilterTahun.getItems().add("Semua Tahun");
        for (int t = tahunIni - 2; t <= tahunIni; t++)
            cbFilterTahun.getItems().add(String.valueOf(t));
        cbFilterTahun.setValue("Semua Tahun");
    }

    // =========================================================
    // SETUP KOLOM TABEL — menggunakan lambda (bukan PropertyValueFactory)
    // agar tidak ada ketergantungan JavaFX di class model TransaksiItem
    // =========================================================
    private void setupKolomTabel() {
        // Setiap kolom diberi lambda yang mengambil nilai dari TransaksiItem
        kolTanggal.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTanggal()));

        kolKategori.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getNamaKategori()));

        // Kolom Tipe: warnai teks sesuai jenis transaksi
        kolTipe.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getTipe()));
        kolTipe.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String tipe, boolean empty) {
                super.updateItem(tipe, empty);
                if (empty || tipe == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(tipe);
                    // Warna teks: hijau untuk pemasukan, merah untuk pengeluaran
                    if (tipe.equals("Pemasukan")) {
                        setStyle("-fx-text-fill: #06d6a0; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #ef476f; -fx-font-weight: bold;");
                    }
                }
            }
        });

        // Kolom Nominal: tampilkan dalam format Rupiah
        kolNominal.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getNominalFormatted()));

        kolKeterangan.setCellValueFactory(
            data -> new javafx.beans.property.SimpleStringProperty(
                data.getValue().getKeterangan()));
    }

    // =========================================================
    // CELL FACTORY KOLOM AKSI — tombol Edit + Hapus di setiap baris
    // =========================================================
    private void setupCellFactoryAksi() {
        kolAksi.setCellFactory(col -> new TableCell<>() {

            // Buat tombol sekali saja per cell, reuse saat scroll
            private final Button btnEdit  = new Button("✏ Edit");
            private final Button btnHapus = new Button("🗑 Hapus");
            private final HBox   hbox     = new HBox(6, btnEdit, btnHapus);

            // Blok inisialisasi: pasang style dan handler sekali
            {
                btnEdit.getStyleClass().add("btn-warning");
                btnHapus.getStyleClass().add("btn-danger");

                // Tombol Edit: isi static field TambahTransaksiController lalu navigasi
                btnEdit.setOnAction(e -> {
                    TransaksiItem item = getTableView().getItems().get(getIndex());
                    // Kirim data ke form TambahTransaksi via static field
                    TambahTransaksiController.idTransaksiEdit = item.getIdTransaksi();
                    TambahTransaksiController.tipeEdit        = item.getTipe();
                    TambahTransaksiController.tanggalEdit     = item.getTanggal();
                    TambahTransaksiController.nominalEdit     = item.getNominal();
                    TambahTransaksiController.keteranganEdit  = item.getKeterangan();
                    // idKategoriEdit: cari dari nama — disederhanakan lewat query di controller
                    TambahTransaksiController.idKategoriEdit  = cariIdKategori(item.getNamaKategori());
                    SceneManager.getInstance().tampilkan(SceneManager.Halaman.TAMBAH);
                });

                // Tombol Hapus: konfirmasi dulu, baru hapus
                btnHapus.setOnAction(e -> {
                    TransaksiItem item = getTableView().getItems().get(getIndex());
                    handleHapusTransaksi(item);
                });
            }

            @Override
            protected void updateItem(Void val, boolean empty) {
                super.updateItem(val, empty);
                // Tampilkan tombol hanya jika baris berisi data
                setGraphic(empty ? null : hbox);
            }
        });
    }

    /**
     * Cari idKategori berdasarkan nama kategori.
     * Dipakai oleh tombol Edit agar form tahu kategori mana yang dipilih.
     */
    private int cariIdKategori(String namaKategori) {
        try {
            var semua = com.myfinance.model.Kategori.getAllKategori();
            for (var k : semua) {
                if (k.getNamaKategori().equals(namaKategori)) return k.getIdKategori();
            }
        } catch (Exception e) { e.printStackTrace(); }
        return 0;
    }

    /**
     * Tampilkan dialog konfirmasi sebelum menghapus transaksi.
     */
    private void handleHapusTransaksi(TransaksiItem item) {
        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Hapus");
        konfirmasi.setHeaderText("Hapus Transaksi");
        konfirmasi.setContentText(
            "Yakin ingin menghapus transaksi berikut?\n"
            + item.getTipe() + " — " + item.getNamaKategori()
            + " — " + item.getNominalFormatted()
            + " (" + item.getTanggal() + ")"
        );

        Optional<ButtonType> hasil = konfirmasi.showAndWait();
        if (hasil.isPresent() && hasil.get() == ButtonType.OK) {
            try {
                boolean berhasil = TransaksiItem.hapusTransaksi(item.getIdTransaksi());
                if (berhasil) {
                    muatData(); // Refresh tabel
                } else {
                    tampilkanInfo("Gagal menghapus transaksi.");
                }
            } catch (Exception e) {
                tampilkanInfo("Error: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    // =========================================================
    // MUAT DATA
    // =========================================================

    /** Dipanggil saat ComboBox filter berubah */
    @FXML
    private void handleFilter() {
        muatData();
    }

    @FXML
    private void handleResetFilter() {
        cbFilterTipe.setValue("Semua");
        cbFilterBulan.setValue("Semua Bulan");
        cbFilterTahun.setValue("Semua Tahun");
        muatData();
    }

    private void muatData() {
        try {
            int idUser = SessionManager.getInstance().getIdUserAktif();

            // Terjemahkan pilihan filter ke nilai yang diterima model
            String tipe  = cbFilterTipe.getValue().equals("Semua") ? null : cbFilterTipe.getValue();
            int    bulan = cbFilterBulan.getValue().equals("Semua Bulan") ? 0
                           : cbFilterBulan.getSelectionModel().getSelectedIndex(); // index 1=Jan dst.
            int    tahun = cbFilterTahun.getValue().equals("Semua Tahun") ? 0
                           : Integer.parseInt(cbFilterTahun.getValue());

            ArrayList<TransaksiItem> daftar =
                TransaksiItem.getSemuaTransaksi(idUser, tipe, bulan, tahun);

            // Masukkan ke ObservableList agar TableView otomatis update
            ObservableList<TransaksiItem> data = FXCollections.observableArrayList(daftar);
            tableRiwayat.setItems(data);

            lblJumlah.setText("Menampilkan " + daftar.size() + " transaksi");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void tampilkanInfo(String pesan) {
        new Alert(Alert.AlertType.INFORMATION, pesan).showAndWait();
    }

    // ── Navigasi Sidebar ──
    @FXML private void handleDashboard()  { SceneManager.getInstance().tampilkan(SceneManager.Halaman.DASHBOARD); }
    @FXML private void handleTambah()    { SceneManager.getInstance().tampilkan(SceneManager.Halaman.TAMBAH); }
    @FXML private void handleRiwayat()   { muatData(); }
    @FXML private void handleStatistik() { SceneManager.getInstance().tampilkan(SceneManager.Halaman.STATISTIK); }
    @FXML private void handleAnggaran()  { SceneManager.getInstance().tampilkan(SceneManager.Halaman.ANGGARAN); }
    @FXML private void handleKategori()  { SceneManager.getInstance().tampilkan(SceneManager.Halaman.KATEGORI); }
    @FXML private void handleLogout()    { SessionManager.getInstance().logout(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.LOGIN); }
}
