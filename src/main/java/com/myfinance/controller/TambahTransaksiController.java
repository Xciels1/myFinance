package com.myfinance.controller;

import com.myfinance.SceneManager;
import com.myfinance.SessionManager;
import com.myfinance.model.Anggaran;
import com.myfinance.model.Kategori;
import com.myfinance.model.Pemasukan;
import com.myfinance.model.Pengeluaran;
import com.myfinance.model.TransaksiItem;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;
import java.util.ResourceBundle;

/**
 * TambahTransaksiController — Menangani form tambah DAN edit transaksi.
 *
 * Fitur baru:
 * - Tampilkan total saldo user di atas form
 * - Validasi anggaran sebelum menyimpan pengeluaran (harus ada anggaran, tidak boleh minus)
 * - Quick-amount bersifat AKUMULATIF (5K + 10K = 15K)
 * - Tombol Reset untuk mengosongkan nominal
 * - Popup konfirmasi sebelum simpan
 */
public class TambahTransaksiController implements Initializable {

    // ── Header & Saldo ──
    @FXML private Label      lblJudul;
    @FXML private Label      lblSaldo;
    @FXML private Label      lblWarningAnggaran;

    // ── Tipe Toggle Buttons ──
    @FXML private Button     btnPemasukan;
    @FXML private Button     btnPengeluaran;

    // ── Kategori FlowPane ──
    @FXML private VBox       vboxKategori;
    @FXML private FlowPane   flowKategori;

    // ── Tanggal ──
    @FXML private DatePicker dpTanggal;

    // ── Nominal + Quick Amount ──
    @FXML private TextField  tfNominal;
    @FXML private FlowPane   flowNominal;
    @FXML private Button     btnReset;

    // ── Keterangan & Error ──
    @FXML private TextField  tfKeterangan;
    @FXML private Label      lblError;

    // ── State internal ──
    private String   tipeAktif     = null;
    private Kategori kategoriAktif = null;
    private Button   chipAktif     = null;
    private double   saldoSaatIni  = 0;

    // ── Daftar nominal cepat ──
    private static final long[] QUICK_AMOUNTS = {
            5_000, 10_000, 15_000, 20_000, 25_000,
            50_000, 100_000, 200_000, 500_000
    };

    // ── Static: data untuk mode Edit ──
    public static int    idTransaksiEdit = 0;
    public static String tipeEdit        = "";
    public static int    idKategoriEdit  = 0;
    public static String tanggalEdit     = "";
    public static double nominalEdit     = 0;
    public static String keteranganEdit  = "";

    // ─────────────────────────────────────────────────────────
    //  INITIALIZE
    // ─────────────────────────────────────────────────────────

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        buatQuickAmountButtons();
        dpTanggal.setValue(LocalDate.now());
        muatSaldo();

        // Mode Edit: pre-fill semua field
        if (idTransaksiEdit > 0) {
            lblJudul.setText("Edit Transaksi");
            setTipe(tipeEdit);
            muatKategori(tipeEdit);
            pilihChipKategoriById(idKategoriEdit);
            dpTanggal.setValue(LocalDate.parse(tanggalEdit));
            tfNominal.setText(String.valueOf((int) nominalEdit));
            tfKeterangan.setText(keteranganEdit);
        }
    }

    /** Muat dan tampilkan total saldo user */
    private void muatSaldo() {
        try {
            int idUser = SessionManager.getInstance().getIdUserAktif();
            saldoSaatIni = Anggaran.getTotalSaldo(idUser);
            lblSaldo.setText(formatRupiah(saldoSaatIni));
            if (saldoSaatIni < 0) {
                lblSaldo.setStyle("-fx-text-fill: #ef476f; -fx-font-size: 22px; -fx-font-weight: bold;");
            } else {
                lblSaldo.setStyle("-fx-text-fill: #06d6a0; -fx-font-size: 22px; -fx-font-weight: bold;");
            }
        } catch (Exception e) {
            lblSaldo.setText("Rp -");
            e.printStackTrace();
        }
    }

    // ─────────────────────────────────────────────────────────
    //  TOGGLE TIPE
    // ─────────────────────────────────────────────────────────

    @FXML
    private void handlePilihPemasukan() {
        setTipe("Pemasukan");
        muatKategori("Pemasukan");
        lblWarningAnggaran.setText("");
    }

    @FXML
    private void handlePilihPengeluaran() {
        setTipe("Pengeluaran");
        muatKategori("Pengeluaran");
        lblWarningAnggaran.setText("");
    }

    private void setTipe(String tipe) {
        tipeAktif = tipe;

        btnPemasukan.getStyleClass().removeAll("tipe-btn-active-masuk", "tipe-btn-active-keluar");
        btnPengeluaran.getStyleClass().removeAll("tipe-btn-active-masuk", "tipe-btn-active-keluar");

        if ("Pemasukan".equals(tipe)) {
            btnPemasukan.getStyleClass().add("tipe-btn-active-masuk");
        } else {
            btnPengeluaran.getStyleClass().add("tipe-btn-active-keluar");
        }

        kategoriAktif = null;
        chipAktif     = null;
        sembunyiError();
    }

    // ─────────────────────────────────────────────────────────
    //  CHIP KATEGORI
    // ─────────────────────────────────────────────────────────

    private void muatKategori(String tipe) {
        flowKategori.getChildren().clear();
        kategoriAktif = null;
        chipAktif     = null;

        try {
            ArrayList<Kategori> list = Kategori.getKategoriByTipe(tipe);
            for (Kategori k : list) {
                Button chip = new Button(k.getNamaKategori());
                chip.getStyleClass().add("chip-btn");
                chip.setUserData(k);
                chip.setCursor(javafx.scene.Cursor.HAND);
                chip.setOnAction(e -> handlePilihKategori(chip, k));
                flowKategori.getChildren().add(chip);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        vboxKategori.setVisible(true);
        vboxKategori.setManaged(true);
    }

    private void handlePilihKategori(Button chip, Kategori k) {
        if (chipAktif != null) {
            chipAktif.getStyleClass().remove("chip-btn-selected");
        }
        kategoriAktif = k;
        chipAktif     = chip;
        chip.getStyleClass().add("chip-btn-selected");
        sembunyiError();

        // Jika pengeluaran, cek dan tampilkan info anggaran kategori ini
        if ("Pengeluaran".equals(tipeAktif)) {
            cekAnggaranKategori(k.getIdKategori());
        } else {
            lblWarningAnggaran.setText("");
        }
    }

    /** Tampilkan sisa anggaran untuk kategori yang dipilih */
    private void cekAnggaranKategori(int idKategori) {
        try {
            int idUser = SessionManager.getInstance().getIdUserAktif();
            int bulan  = LocalDate.now().getMonthValue();
            int tahun  = LocalDate.now().getYear();
            Anggaran ang = Anggaran.getAnggaranByKategori(idUser, idKategori, bulan, tahun);

            if (ang == null) {
                lblWarningAnggaran.setText(
                    "Belum ada anggaran untuk kategori ini. Tambahkan anggaran terlebih dahulu.");
                lblWarningAnggaran.setStyle("-fx-text-fill: #ef476f;");
            } else {
                double sisa = ang.hitungSisaAnggaran();
                if (sisa <= 0) {
                    lblWarningAnggaran.setText(
                        "Anggaran sudah habis! Limit: " + formatRupiah(ang.getLimitNominal()) +
                        " | Terpakai: " + formatRupiah(ang.getTotalPengeluaran()));
                    lblWarningAnggaran.setStyle("-fx-text-fill: #ef476f;");
                } else {
                    lblWarningAnggaran.setText(
                        "Sisa anggaran: " + formatRupiah(sisa) +
                        " (dari " + formatRupiah(ang.getLimitNominal()) + ")");
                    lblWarningAnggaran.setStyle("-fx-text-fill: #06d6a0;");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void pilihChipKategoriById(int idKategori) {
        for (javafx.scene.Node node : flowKategori.getChildren()) {
            if (node instanceof Button chip) {
                Object data = chip.getUserData();
                if (data instanceof Kategori k && k.getIdKategori() == idKategori) {
                    handlePilihKategori(chip, k);
                    break;
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────
    //  QUICK-AMOUNT BUTTONS (AKUMULATIF)
    // ─────────────────────────────────────────────────────────

    private void buatQuickAmountButtons() {
        for (long amount : QUICK_AMOUNTS) {
            Button qbtn = new Button(formatNominalSingkat(amount));
            qbtn.getStyleClass().add("quick-btn");
            qbtn.setUserData(amount);
            qbtn.setCursor(javafx.scene.Cursor.HAND);
            // Setiap klik MENAMBAHKAN ke nominal yang sudah ada
            qbtn.setOnAction(e -> handleQuickAmount(amount));
            flowNominal.getChildren().add(qbtn);
        }
    }

    /**
     * Tambahkan amount ke nilai nominal yang sudah ada (akumulatif).
     * Jika field kosong atau bukan angka, mulai dari 0.
     */
    private void handleQuickAmount(long amount) {
        String current = tfNominal.getText().trim();
        double existing = 0;
        try {
            if (!current.isEmpty()) {
                existing = Double.parseDouble(current);
            }
        } catch (NumberFormatException ignored) {}

        double hasil = existing + amount;
        // Tampilkan sebagai integer jika tidak ada desimal
        if (hasil == (long) hasil) {
            tfNominal.setText(String.valueOf((long) hasil));
        } else {
            tfNominal.setText(String.valueOf(hasil));
        }
        sembunyiError();
    }

    /** Reset nominal ke kosong */
    @FXML
    private void handleResetNominal() {
        tfNominal.clear();
        sembunyiError();
    }

    private String formatNominalSingkat(long nominal) {
        if (nominal >= 1_000_000) return (nominal / 1_000_000) + "JT";
        if (nominal >= 1_000)     return (nominal / 1_000) + "K";
        return String.valueOf(nominal);
    }

    // ─────────────────────────────────────────────────────────
    //  SIMPAN (dengan validasi anggaran)
    // ─────────────────────────────────────────────────────────

    @FXML
    private void handleSimpan() {
        // Validasi input dasar
        if (tipeAktif == null) {
            tampilkanError("Pilih jenis transaksi terlebih dahulu.");
            return;
        }
        if (kategoriAktif == null) {
            tampilkanError("Pilih kategori transaksi.");
            return;
        }
        if (dpTanggal.getValue() == null) {
            tampilkanError("Pilih tanggal transaksi.");
            return;
        }
        String inputNominal = tfNominal.getText().trim();
        if (inputNominal.isEmpty()) {
            tampilkanError("Nominal tidak boleh kosong.");
            return;
        }
        double nominal;
        try {
            nominal = Double.parseDouble(inputNominal);
            if (nominal <= 0) { tampilkanError("Nominal harus lebih dari 0."); return; }
        } catch (NumberFormatException e) {
            tampilkanError("Nominal harus berupa angka (tanpa titik/koma).");
            return;
        }

        // ── VALIDASI ANGGARAN (khusus pengeluaran, bukan mode edit) ──
        if ("Pengeluaran".equals(tipeAktif) && idTransaksiEdit == 0) {
            try {
                int idUser = SessionManager.getInstance().getIdUserAktif();
                int bulan  = LocalDate.now().getMonthValue();
                int tahun  = LocalDate.now().getYear();
                Anggaran ang = Anggaran.getAnggaranByKategori(
                        idUser, kategoriAktif.getIdKategori(), bulan, tahun);

                if (ang == null) {
                    // Belum ada anggaran untuk kategori ini
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Anggaran Belum Diatur");
                    alert.setHeaderText("Tidak ada anggaran untuk kategori ini");
                    alert.setContentText(
                        "Anda belum mengatur anggaran untuk kategori \"" +
                        kategoriAktif.getNamaKategori() + "\".\n\n" +
                        "Silakan buka halaman Anggaran dan tambahkan limit terlebih dahulu " +
                        "sebelum mencatat pengeluaran pada kategori ini.");
                    alert.showAndWait();
                    return;
                }

                double sisa = ang.hitungSisaAnggaran();
                if (nominal > sisa) {
                    // Akan melebihi anggaran
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Anggaran Tidak Cukup");
                    alert.setHeaderText("Pengeluaran melebihi sisa anggaran");
                    alert.setContentText(String.format(
                        "Kategori \"%s\":\n" +
                        "  Sisa anggaran : %s\n" +
                        "  Nominal input : %s\n\n" +
                        "Kurangi nominal atau tambah limit anggaran di halaman Anggaran.",
                        kategoriAktif.getNamaKategori(),
                        formatRupiah(sisa),
                        formatRupiah(nominal)
                    ));
                    alert.showAndWait();
                    return;
                }
            } catch (Exception e) {
                tampilkanError("Gagal memeriksa anggaran: " + e.getMessage());
                e.printStackTrace();
                return;
            }
        }

        String tanggal    = dpTanggal.getValue().toString();
        String keterangan = tfKeterangan.getText().trim();
        String labelAksi  = (idTransaksiEdit > 0) ? "memperbarui" : "menambahkan";

        // ── POPUP KONFIRMASI ──
        String pesanKonfirmasi = String.format(
            "Apakah Anda yakin ingin %s transaksi berikut?\n\n" +
            "  Jenis       : %s\n" +
            "  Kategori    : %s\n" +
            "  Tanggal     : %s\n" +
            "  Nominal     : %s\n" +
            "  Keterangan  : %s",
            labelAksi,
            tipeAktif,
            kategoriAktif.getNamaKategori(),
            tanggal,
            formatRupiah(nominal),
            keterangan.isEmpty() ? "-" : keterangan
        );

        Alert konfirmasi = new Alert(Alert.AlertType.CONFIRMATION);
        konfirmasi.setTitle("Konfirmasi Transaksi");
        konfirmasi.setHeaderText("Konfirmasi " + (idTransaksiEdit > 0 ? "Edit" : "Tambah") + " Transaksi");
        konfirmasi.setContentText(pesanKonfirmasi);

        ButtonType btnYa    = new ButtonType("Ya, Simpan", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnBatal = new ButtonType("Batal", ButtonBar.ButtonData.CANCEL_CLOSE);
        konfirmasi.getButtonTypes().setAll(btnYa, btnBatal);

        Optional<ButtonType> hasil = konfirmasi.showAndWait();
        if (hasil.isEmpty() || hasil.get() != btnYa) {
            return;
        }

        // ── PROSES SIMPAN ──
        int idKategori = kategoriAktif.getIdKategori();
        int idUser     = SessionManager.getInstance().getIdUserAktif();

        try {
            boolean berhasil;
            if (idTransaksiEdit > 0) {
                berhasil = TransaksiItem.updateTransaksi(
                        idTransaksiEdit, idKategori, tanggal, nominal, keterangan);
            } else {
                if ("Pemasukan".equals(tipeAktif)) {
                    berhasil = new Pemasukan(idUser, idKategori, tanggal, nominal, keterangan)
                            .simpanTransaksi();
                } else {
                    berhasil = new Pengeluaran(idUser, idKategori, tanggal, nominal, keterangan)
                            .simpanTransaksi();
                }
            }

            if (berhasil) {
                resetModeEdit();
                SceneManager.getInstance().tampilkan(SceneManager.Halaman.RIWAYAT);
            } else {
                tampilkanError("Gagal menyimpan transaksi. Coba lagi.");
            }
        } catch (Exception e) {
            tampilkanError("Terjadi kesalahan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleBatal() {
        resetModeEdit();
        SceneManager.getInstance().tampilkan(SceneManager.Halaman.RIWAYAT);
    }

    // ─────────────────────────────────────────────────────────
    //  HELPER
    // ─────────────────────────────────────────────────────────

    private void tampilkanError(String pesan) {
        lblError.setText("⚠ " + pesan);
        lblError.setVisible(true);
    }

    private void sembunyiError() {
        lblError.setText("");
        lblError.setVisible(false);
    }

    private String formatRupiah(double n) {
        return "Rp " + String.format("%,.0f", n);
    }

    public static void resetModeEdit() {
        idTransaksiEdit = 0;
        tipeEdit        = "";
        idKategoriEdit  = 0;
        tanggalEdit     = "";
        nominalEdit     = 0;
        keteranganEdit  = "";
    }

    // ── Navigasi Sidebar ──
    @FXML private void handleDashboard()  { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.DASHBOARD); }
    @FXML private void handleTambah()     { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.TAMBAH); }
    @FXML private void handleRiwayat()    { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.RIWAYAT); }
    @FXML private void handleStatistik()  { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.STATISTIK); }
    @FXML private void handleAnggaran()   { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.ANGGARAN); }
    @FXML private void handleKategori()   { resetModeEdit(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.KATEGORI); }
    @FXML private void handleLogout()     { resetModeEdit(); SessionManager.getInstance().logout(); SceneManager.getInstance().tampilkan(SceneManager.Halaman.LOGIN); }
}
