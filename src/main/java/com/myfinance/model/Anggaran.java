package com.myfinance.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Class Anggaran - Menetapkan batas (limit) pengeluaran per kategori.
 * Digunakan untuk memunculkan peringatan (Alert) di Dashboard
 * jika pengeluaran pada kategori tertentu sudah melampaui limit.
 */
public class Anggaran {

    // ===================== Atribut =====================
    private int    idAnggaran;
    private int    idUser;
    private int    idKategori;
    private double limitNominal;

    // Atribut tambahan untuk kemudahan tampilan di UI
    // (diisi saat fetch dari DB dengan JOIN ke tabel kategori)
    private String namaKategori;
    private double totalPengeluaran; // Pengeluaran aktual bulan ini

    // ===================== Constructor =====================
    public Anggaran(int idAnggaran, int idUser, int idKategori,
                    double limitNominal, String namaKategori) {
        this.idAnggaran   = idAnggaran;
        this.idUser       = idUser;
        this.idKategori   = idKategori;
        this.limitNominal = limitNominal;
        this.namaKategori = namaKategori;
    }

    // ===================== Getters & Setters =====================
    public int    getIdAnggaran()      { return idAnggaran; }
    public int    getIdUser()          { return idUser; }
    public int    getIdKategori()      { return idKategori; }
    public double getLimitNominal()    { return limitNominal; }
    public String getNamaKategori()    { return namaKategori; }
    public double getTotalPengeluaran(){ return totalPengeluaran; }

    public void setLimitNominal(double limit)        { this.limitNominal = limit; }
    public void setTotalPengeluaran(double total)    { this.totalPengeluaran = total; }

    // ===================== Logika Bisnis =====================

    /**
     * Hitung sisa anggaran (limit dikurangi pengeluaran aktual).
     * Nilai negatif berarti sudah melampaui limit.
     */
    public double hitungSisaAnggaran() {
        return limitNominal - totalPengeluaran;
    }

    /**
     * Cek apakah pengeluaran sudah melampaui limit anggaran.
     *
     * @return true jika melampaui (over budget), false jika masih aman
     */
    public boolean cekStatusAnggaran() {
        return totalPengeluaran > limitNominal;
    }

    // ===================== Operasi Database (Static) =====================

    /**
     * Simpan atau perbarui limit anggaran untuk kategori tertentu.
     * Menggunakan INSERT OR REPLACE agar tidak duplikat
     * (karena ada constraint UNIQUE pada id_user + id_kategori).
     */
    public static boolean setLimit(int idUser, int idKategori, double limit)
            throws SQLException {
        String sql  = "INSERT OR REPLACE INTO anggaran (id_user, id_kategori, limit_nominal) "
                    + "VALUES (?, ?, ?)";
        int    rows = Database.getInstance().executeUpdate(sql, idUser, idKategori, limit);
        return rows > 0;
    }

    /**
     * Hapus anggaran (limit) untuk kategori tertentu.
     */
    public static boolean hapusAnggaran(int idAnggaran) throws SQLException {
        String sql  = "DELETE FROM anggaran WHERE id_anggaran = ?";
        int    rows = Database.getInstance().executeUpdate(sql, idAnggaran);
        return rows > 0;
    }

    /**
     * Ambil semua anggaran milik user beserta total pengeluaran aktual bulan ini.
     * Menggunakan LEFT JOIN ke transaksi agar kategori yang belum ada
     * transaksinya tetap muncul dengan total = 0.
     *
     * Data ini dipakai Controller Dashboard untuk cek alert anggaran.
     *
     * @param idUser ID pengguna yang sedang login
     * @param bulan  Nomor bulan (1-12) untuk kalkulasi pengeluaran aktual
     * @param tahun  Tahun untuk kalkulasi pengeluaran aktual
     */
    public static ArrayList<Anggaran> getAllAnggaranDenganRealisasi(
            int idUser, int bulan, int tahun) throws SQLException {

        ArrayList<Anggaran> daftar = new ArrayList<>();

        // Query mengambil limit + total pengeluaran aktual bulan ini per kategori
        String bulanStr = String.format("%02d", bulan);
        String tahunStr = String.valueOf(tahun);

        String sql =
            "SELECT a.id_anggaran, a.id_user, a.id_kategori, a.limit_nominal, "
          + "       k.nama_kategori, "
          + "       COALESCE(SUM(t.nominal), 0) AS total_pengeluaran "
          + "FROM anggaran a "
          + "JOIN kategori k ON a.id_kategori = k.id_kategori "
          + "LEFT JOIN transaksi t "
          + "       ON t.id_kategori = a.id_kategori "
          + "      AND t.id_user = a.id_user "
          + "      AND strftime('%m', t.tanggal) = ? "
          + "      AND strftime('%Y', t.tanggal) = ? "
          + "WHERE a.id_user = ? "
          + "GROUP BY a.id_anggaran, a.id_kategori";

        ResultSet rs = Database.getInstance().executeQuery(
                           sql, bulanStr, tahunStr, idUser);

        while (rs.next()) {
            Anggaran ang = new Anggaran(
                rs.getInt("id_anggaran"),
                rs.getInt("id_user"),
                rs.getInt("id_kategori"),
                rs.getDouble("limit_nominal"),
                rs.getString("nama_kategori")
            );
            // Set total pengeluaran aktual agar bisa dicek di Dashboard
            ang.setTotalPengeluaran(rs.getDouble("total_pengeluaran"));
            daftar.add(ang);
        }
        return daftar;
    }

    /**
     * Ambil semua anggaran milik user (tanpa realisasi) - untuk halaman Anggaran.
     */
    public static ArrayList<Anggaran> getAllAnggaran(int idUser) throws SQLException {
        ArrayList<Anggaran> daftar = new ArrayList<>();
        String sql = "SELECT a.*, k.nama_kategori "
                   + "FROM anggaran a "
                   + "JOIN kategori k ON a.id_kategori = k.id_kategori "
                   + "WHERE a.id_user = ? "
                   + "ORDER BY k.nama_kategori";
        ResultSet rs = Database.getInstance().executeQuery(sql, idUser);

        while (rs.next()) {
            daftar.add(new Anggaran(
                rs.getInt("id_anggaran"),
                rs.getInt("id_user"),
                rs.getInt("id_kategori"),
                rs.getDouble("limit_nominal"),
                rs.getString("nama_kategori")
            ));
        }
        return daftar;
    }

    @Override
    public String toString() {
        return namaKategori + " - Limit: Rp " + String.format("%,.0f", limitNominal);
    }
}
