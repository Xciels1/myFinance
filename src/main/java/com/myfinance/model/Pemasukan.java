package com.myfinance.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Class Pemasukan - Turunan (extends) dari Transaksi.
 * Merepresentasikan transaksi pemasukan (uang masuk).
 *
 * Inheritance: Pemasukan --extends--> Transaksi
 */
public class Pemasukan extends Transaksi {

    // ===================== Constructor =====================
    public Pemasukan(int idTransaksi, int idUser, int idKategori,
                     String tanggal, double nominal, String keterangan) {
        super(idTransaksi, idUser, idKategori, tanggal, nominal, keterangan);
    }

    public Pemasukan(int idUser, int idKategori,
                     String tanggal, double nominal, String keterangan) {
        super(idUser, idKategori, tanggal, nominal, keterangan);
    }

    // ===================== Implementasi Method Abstract =====================

    /**
     * Simpan data pemasukan baru ke tabel transaksi.
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
     * Hapus pemasukan dari database berdasarkan ID transaksi.
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
        return "[Pemasukan] Rp " + String.format("%,.0f", nominal)
               + " | " + tanggal
               + (keterangan != null && !keterangan.isEmpty() ? " | " + keterangan : "");
    }

    // ===================== Kalkulasi (Static) =====================

    /**
     * Hitung TOTAL pemasukan milik seorang user.
     * JOIN dengan tabel kategori untuk memastikan tipe = 'Pemasukan'.
     *
     * @param idUser ID pengguna yang sedang login
     * @return Total nominal pemasukan
     */
    public static double hitungTotalPemasukan(int idUser) throws SQLException {
        String sql = "SELECT COALESCE(SUM(t.nominal), 0) "
                   + "FROM transaksi t "
                   + "JOIN kategori k ON t.id_kategori = k.id_kategori "
                   + "WHERE t.id_user = ? AND k.tipe = 'Pemasukan'";
        ResultSet rs = Database.getInstance().executeQuery(sql, idUser);
        return rs.next() ? rs.getDouble(1) : 0;
    }

    /**
     * Ambil semua data pemasukan milik user dalam ArrayList.
     */
    public static ArrayList<Pemasukan> getAllPemasukan(int idUser) throws SQLException {
        ArrayList<Pemasukan> daftar = new ArrayList<>();
        String sql = "SELECT t.* FROM transaksi t "
                   + "JOIN kategori k ON t.id_kategori = k.id_kategori "
                   + "WHERE t.id_user = ? AND k.tipe = 'Pemasukan' "
                   + "ORDER BY t.tanggal DESC";
        ResultSet rs = Database.getInstance().executeQuery(sql, idUser);

        while (rs.next()) {
            daftar.add(new Pemasukan(
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
