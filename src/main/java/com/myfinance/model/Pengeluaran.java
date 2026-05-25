package com.myfinance.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class Pengeluaran - Turunan (extends) dari Transaksi.
 * Merepresentasikan transaksi pengeluaran (uang keluar).
 *
 * Inheritance: Pengeluaran --extends--> Transaksi
 */
public class Pengeluaran extends Transaksi {

    // ===================== Constructor =====================
    public Pengeluaran(int idTransaksi, int idUser, int idKategori,
                       String tanggal, double nominal, String keterangan) {
        super(idTransaksi, idUser, idKategori, tanggal, nominal, keterangan);
    }

    public Pengeluaran(int idUser, int idKategori,
                       String tanggal, double nominal, String keterangan) {
        super(idUser, idKategori, tanggal, nominal, keterangan);
    }

    // ===================== Implementasi Method Abstract =====================

    /**
     * Simpan data pengeluaran baru ke tabel transaksi.
     */
    @Override
    public boolean simpanTransaksi() throws SQLException {
        String sql  = "INSERT INTO transaksi (id_user, id_kategori, tanggal, nominal, keterangan) "
                    + "VALUES (?, ?, ?, ?, ?)";
        int    rows = Database.getInstance().executeUpdate(
                          sql, idUser, idKategori, tanggal, nominal, keterangan);
        return rows > 0;
    }

    /**
     * Hapus pengeluaran dari database berdasarkan ID transaksi.
     */
    @Override
    public boolean hapusTransaksi(int idTransaksi) throws SQLException {
        String sql  = "DELETE FROM transaksi WHERE id_transaksi = ?";
        int    rows = Database.getInstance().executeUpdate(sql, idTransaksi);
        return rows > 0;
    }

    /**
     * Kembalikan ringkasan detail transaksi sebagai String.
     */
    @Override
    public String getDetailTransaksi() {
        return "[Pengeluaran] Rp " + String.format("%,.0f", nominal)
               + " | " + tanggal
               + (keterangan != null && !keterangan.isEmpty() ? " | " + keterangan : "");
    }

    // ===================== Kalkulasi (Static) =====================

    /**
     * Hitung TOTAL pengeluaran milik seorang user.
     */
    public static double hitungTotalPengeluaran(int idUser) throws SQLException {
        String sql = "SELECT COALESCE(SUM(t.nominal), 0) "
                   + "FROM transaksi t "
                   + "JOIN kategori k ON t.id_kategori = k.id_kategori "
                   + "WHERE t.id_user = ? AND k.tipe = 'Pengeluaran'";
        ResultSet rs = Database.getInstance().executeQuery(sql, idUser);
        return rs.next() ? rs.getDouble(1) : 0;
    }

    /**
     * Hitung total pengeluaran per kategori menggunakan HashMap.
     * HashMap digunakan untuk memetakan nama kategori -> total nominal.
     * Data ini dipakai oleh BarChart di halaman Statistik dan Dashboard.
     *
     * @param idUser  ID pengguna
     * @param bulan   Nomor bulan (1-12), 0 = semua bulan
     * @param tahun   Tahun (misal 2026), 0 = semua tahun
     * @return HashMap<namaKategori, totalNominal>
     */
    public static HashMap<String, Double> hitungPengeluaranPerKategori(
            int idUser, int bulan, int tahun) throws SQLException {

        HashMap<String, Double> hasil = new HashMap<>();

        // Query dasar: gabungkan transaksi dan kategori
        StringBuilder sql = new StringBuilder(
            "SELECT k.nama_kategori, SUM(t.nominal) AS total "
          + "FROM transaksi t "
          + "JOIN kategori k ON t.id_kategori = k.id_kategori "
          + "WHERE t.id_user = ? AND k.tipe = 'Pengeluaran' "
        );

        // Filter bulan dan tahun jika diberikan
        if (bulan > 0 && tahun > 0) {
            sql.append("AND strftime('%m', t.tanggal) = ? ")
               .append("AND strftime('%Y', t.tanggal) = ? ");
        } else if (tahun > 0) {
            sql.append("AND strftime('%Y', t.tanggal) = ? ");
        }

        sql.append("GROUP BY k.id_kategori, k.nama_kategori");

        // Siapkan parameter sesuai kondisi filter
        ResultSet rs;
        if (bulan > 0 && tahun > 0) {
            // Format bulan jadi 2 digit: "01", "02", dst
            String bulanStr = String.format("%02d", bulan);
            String tahunStr = String.valueOf(tahun);
            rs = Database.getInstance().executeQuery(sql.toString(), idUser, bulanStr, tahunStr);
        } else if (tahun > 0) {
            rs = Database.getInstance().executeQuery(sql.toString(), idUser, String.valueOf(tahun));
        } else {
            rs = Database.getInstance().executeQuery(sql.toString(), idUser);
        }

        while (rs.next()) {
            hasil.put(rs.getString("nama_kategori"), rs.getDouble("total"));
        }
        return hasil;
    }

    /**
     * Ambil semua data pengeluaran milik user dalam ArrayList.
     */
    public static ArrayList<Pengeluaran> getAllPengeluaran(int idUser) throws SQLException {
        ArrayList<Pengeluaran> daftar = new ArrayList<>();
        String sql = "SELECT t.* FROM transaksi t "
                   + "JOIN kategori k ON t.id_kategori = k.id_kategori "
                   + "WHERE t.id_user = ? AND k.tipe = 'Pengeluaran' "
                   + "ORDER BY t.tanggal DESC";
        ResultSet rs = Database.getInstance().executeQuery(sql, idUser);

        while (rs.next()) {
            daftar.add(new Pengeluaran(
                rs.getInt("id_transaksi"),
                rs.getInt("id_user"),
                rs.getInt("id_kategori"),
                rs.getString("tanggal"),
                rs.getDouble("nominal"),
                rs.getString("keterangan")
            ));
        }
        return daftar;
    }
}
