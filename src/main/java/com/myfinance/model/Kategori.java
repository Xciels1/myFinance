package com.myfinance.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Class Kategori - Merepresentasikan pengelompokan transaksi.
 * Contoh: Makan, Transport, Gaji, dll.
 *
 * Relasi: Kategori ke Transaksi bersifat Agregasi (1 kategori, banyak transaksi).
 */
public class Kategori {

    // ===================== Atribut =====================
    private int    idKategori;
    private String namaKategori;
    private String tipe; // 'Pemasukan' atau 'Pengeluaran'

    // ===================== Constructor =====================
    public Kategori(int idKategori, String namaKategori, String tipe) {
        this.idKategori   = idKategori;
        this.namaKategori = namaKategori;
        this.tipe         = tipe;
    }

    // ===================== Getters =====================
    public int    getIdKategori()   { return idKategori; }
    public String getNamaKategori() { return namaKategori; }
    public String getTipe()         { return tipe; }

    // ===================== Operasi Database (Static) =====================

    /**
     * Ambil semua kategori dari database.
     * Hasilnya disimpan dalam ArrayList<Kategori>.
     */
    public static ArrayList<Kategori> getAllKategori() throws SQLException {
        ArrayList<Kategori> daftarKategori = new ArrayList<>();
        String sql = "SELECT * FROM kategori ORDER BY tipe, nama_kategori";
        ResultSet rs = Database.getInstance().executeQuery(sql);

        while (rs.next()) {
            daftarKategori.add(new Kategori(
                rs.getInt("id_kategori"),
                rs.getString("nama_kategori"),
                rs.getString("tipe")
            ));
        }
        return daftarKategori;
    }

    /**
     * Ambil kategori berdasarkan tipe tertentu ('Pemasukan' atau 'Pengeluaran').
     */
    public static ArrayList<Kategori> getKategoriByTipe(String tipe) throws SQLException {
        ArrayList<Kategori> hasil = new ArrayList<>();
        String sql = "SELECT * FROM kategori WHERE tipe = ? ORDER BY nama_kategori";
        ResultSet rs = Database.getInstance().executeQuery(sql, tipe);

        while (rs.next()) {
            hasil.add(new Kategori(
                rs.getInt("id_kategori"),
                rs.getString("nama_kategori"),
                rs.getString("tipe")
            ));
        }
        return hasil;
    }

    /**
     * Tambahkan kategori baru ke database (CREATE).
     */
    public static boolean tambahKategori(String namaKategori, String tipe) throws SQLException {
        String sql  = "INSERT INTO kategori (nama_kategori, tipe) VALUES (?, ?)";
        int    rows = Database.getInstance().executeUpdate(sql, namaKategori, tipe);
        return rows > 0;
    }

    /**
     * Ubah nama dan tipe kategori yang sudah ada (UPDATE).
     */
    public static boolean updateKategori(int idKategori, String namaBaru, String tipeBaru)
            throws SQLException {
        String sql  = "UPDATE kategori SET nama_kategori = ?, tipe = ? WHERE id_kategori = ?";
        int    rows = Database.getInstance().executeUpdate(sql, namaBaru, tipeBaru, idKategori);
        return rows > 0;
    }

    /**
     * Hapus kategori dari database (DELETE).
     * Catatan: Akan gagal jika kategori masih dipakai oleh transaksi (karena FK).
     */
    public static boolean hapusKategori(int idKategori) throws SQLException {
        String sql  = "DELETE FROM kategori WHERE id_kategori = ?";
        int    rows = Database.getInstance().executeUpdate(sql, idKategori);
        return rows > 0;
    }

    // Untuk ditampilkan di ComboBox JavaFX
    @Override
    public String toString() {
        return namaKategori;
    }
}
