package com.myfinance.controller;

import com.myfinance.SceneManager;
import com.myfinance.SessionManager;
import com.myfinance.model.Kategori;
import com.myfinance.model.Pemasukan;
import com.myfinance.model.Pengeluaran;
import com.myfinance.model.TransaksiItem;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.ResourceBundle;

/**
 * TambahTransaksiController — Menangani form tambah DAN edit transaksi.
 *
 * Mode Edit:
 *   RiwayatController set idTransaksiEdit > 0 dan isi field sebelum navigasi,
 *   lalu controller ini mendeteksi mode edit di initialize() dan pre-fill form.
 */
public class TambahTransaksiController implements Initializable {

    @FXML private Label      lblJudul;
    @FXML private ComboBox<String>   cbTipe;
    @FXML private ComboBox<Kategori> cbKategori;
    @FXML private DatePicker dpTanggal;
    @FXML private TextField  tfNominal;
    @FXML private TextField  tfKeterangan;
    @FXML private Label      lblError;

    // ── Static: data untuk mode Edit (diset oleh RiwayatController) ──
    public static int         idTransaksiEdit  = 0;   // 0 = mode Tambah
    public static String      tipeEdit         = "";
    public static int         idKategoriEdit   = 0;
    public static String      tanggalEdit      = "";
    public static double      nominalEdit      = 0;
    public static String      keteranganEdit   = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Isi ComboBox tipe
        cbTipe.getItems().addAll("Pemasukan", "Pengeluaran");

        // Jika mode Edit, pre-fill semua field
        if (idTransaksiEdit > 0) {
            lblJudul.setText("Edit Transaksi");
            cbTipe.setValue(tipeEdit);
            muatKategori(tipeEdit);      // muat kategori sesuai tipe
            pilihKategoriById(idKategoriEdit);
            dpTanggal.setValue(LocalDate.parse(tanggalEdit));
            tfNominal.setText(String.valueOf((int) nominalEdit));
            tfKeterangan.setText(keteranganEdit);
        } else {
            // Mode tambah: set tanggal ke hari ini
            dpTanggal.setValue(LocalDate.now());
        }
    }

    /**
     * Dipanggil saat tipe berubah (Pemasukan / Pengeluaran).
     * Reload isi ComboBox kategori sesuai tipe.
     */
    @FXML
    private void handleTipeChanged() {
        String tipe = cbTipe.getValue();
        if (tipe != null) {
            muatKategori(tipe);
        }
    }

    /** Isi cbKategori berdasarkan tipe yang dipilih */
    private void muatKategori(String tipe) {
        cbKategori.getItems().clear();
        try {
            ArrayList<Kategori> list = Kategori.getKategoriByTipe(tipe);
            cbKategori.getItems().addAll(list);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Pilih item di cbKategori berdasarkan idKategori (untuk mode edit) */
    private void pilihKategoriById(int idKategori) {
        for (Kategori k : cbKategori.getItems()) {
            if (k.getIdKategori() == idKategori) {
                cbKategori.setValue(k);
                break;
            }
        }
    }

    /** Dipanggil saat tombol "Simpan" ditekan */
    @FXML
    private void handleSimpan() {
        // ── Validasi semua field ──
        if (cbTipe.getValue() == null) {
            tampilkanError("Pilih jenis transaksi terlebih dahulu.");
            return;
        }
        if (cbKategori.getValue() == null) {
            tampilkanError("Pilih kategori transaksi.");
            return;
        }
        if (dpTanggal.getValue() == null) {
            tampilkanError("Pilih tanggal transaksi.");
            return;
        }
        if (tfNominal.getText().trim().isEmpty()) {
            tampilkanError("Nominal tidak boleh kosong.");
            return;
        }

        // Validasi nominal harus angka positif
        double nominal;
        try {
            nominal = Double.parseDouble(tfNominal.getText().trim());
            if (nominal <= 0) {
                tampilkanError("Nominal harus lebih dari 0.");
                return;
            }
        } catch (NumberFormatException e) {
            tampilkanError("Nominal harus berupa angka (tanpa titik/koma).");
            return;
        }

        // Kumpulkan semua nilai dari form
        String   tipe       = cbTipe.getValue();
        int      idKategori = cbKategori.getValue().getIdKategori();
        String   tanggal    = dpTanggal.getValue().toString(); // format YYYY-MM-DD
        String   keterangan = tfKeterangan.getText().trim();
        int      idUser     = SessionManager.getInstance().getIdUserAktif();

        try {
            boolean berhasil;

            if (idTransaksiEdit > 0) {
                // ── MODE EDIT: update data yang sudah ada ──
                berhasil = TransaksiItem.updateTransaksi(
                               idTransaksiEdit, idKategori, tanggal, nominal, keterangan);
            } else {
                // ── MODE TAMBAH: buat objek baru lalu simpan ──
                if (tipe.equals("Pemasukan")) {
                    Pemasukan p = new Pemasukan(idUser, idKategori, tanggal, nominal, keterangan);
                    berhasil = p.simpanTransaksi();
                } else {
                    Pengeluaran p = new Pengeluaran(idUser, idKategori, tanggal, nominal, keterangan);
                    berhasil = p.simpanTransaksi();
                }
            }

            if (berhasil) {
                resetModeEdit();          // bersihkan static field
                SceneManager.getInstance().tampilkan(SceneManager.Halaman.RIWAYAT);
            } else {
                tampilkanError("Gagal menyimpan transaksi. Coba lagi.");
            }

        } catch (Exception e) {
            tampilkanError("Terjadi kesalahan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /** Batal: bersihkan mode edit lalu kembali ke Riwayat */
    @FXML
    private void handleBatal() {
        resetModeEdit();
        SceneManager.getInstance().tampilkan(SceneManager.Halaman.RIWAYAT);
    }

    /** Reset semua static field mode edit ke nilai awal */
    public static void resetModeEdit() {
        idTransaksiEdit = 0;
        tipeEdit        = "";
        idKategoriEdit  = 0;
        tanggalEdit     = "";
        nominalEdit     = 0;
        keteranganEdit  = "";
    }

    private void tampilkanError(String pesan) {
        lblError.setText("⚠ " + pesan);
        lblError.setVisible(true);
    }

    // ── Navigasi Sidebar ──
    @FXML private void handleDashboard()  { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.DASHBOARD); }
    @FXML private void handleTambah()     { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.TAMBAH); }
    @FXML private void handleRiwayat()   { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.RIWAYAT); }
    @FXML private void handleStatistik() { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.STATISTIK); }
    @FXML private void handleAnggaran()  { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.ANGGARAN); }
    @FXML private void handleKategori()  { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.KATEGORI); }
    @FXML private void handleLogout()    { resetModeEdit(); SessionManager.getInstance().logout(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.LOGIN); }
}
