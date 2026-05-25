package com.myfinance.model;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

/**
 * Class TransaksiItem - Data Transfer Object (DTO) untuk TableView Riwayat.
 *
 * Class ini TIDAK mengandung kode JavaFX sama sekali.
 * Binding ke TableView dilakukan di Controller menggunakan lambda setCellValueFactory.
 *
 * Berisi data gabungan dari tabel transaksi + kategori dalam satu baris tabel.
 */
public class TransaksiItem {

    // ===================== Atribut =====================
    private int    idTransaksi;
    private String tanggal;
    private String namaKategori;
    private String tipe;          // 'Pemasukan' atau 'Pengeluaran'
    private double nominal;
    private String keterangan;

    // ===================== Constructor =====================
    public TransaksiItem(int idTransaksi, String tanggal, String namaKategori,
                         String tipe, double nominal, String keterangan) {
        this.idTransaksi  = idTransaksi;
        this.tanggal      = tanggal;
        this.namaKategori = namaKategori;
        this.tipe         = tipe;
        this.nominal      = nominal;
        this.keterangan   = keterangan != null ? keterangan : "-";
    }

    // ===================== Getters =====================
    public int    getIdTransaksi()      { return idTransaksi; }
    public String getTanggal()          { return tanggal; }
    public String getNamaKategori()     { return namaKategori; }
    public String getTipe()             { return tipe; }
    public double getNominal()          { return nominal; }
    public String getKeterangan()       { return keterangan; }

    /** Nominal dalam format Rupiah untuk kolom tabel */
    public String getNominalFormatted() {
        return "Rp " + String.format("%,.0f", nominal);
    }

    // ===================== Operasi Database (Static) =====================

    /**
     * Ambil semua transaksi milik user dengan filter opsional.
     *
     * @param idUser ID pengguna aktif
     * @param tipe   Filter tipe: 'Pemasukan', 'Pengeluaran', atau null untuk semua
     * @param bulan  Nomor bulan 1-12, 0 untuk semua
     * @param tahun  Tahun misal 2026, 0 untuk semua
     */
    public static ArrayList<TransaksiItem> getSemuaTransaksi(
            int idUser, String tipe, int bulan, int tahun) throws SQLException {

        ArrayList<TransaksiItem> daftar = new ArrayList<>();
        ArrayList<Object> params = new ArrayList<>();

        StringBuilder sql = new StringBuilder(
            "SELECT t.id_transaksi, t.tanggal, k.nama_kategori, "
          + "       k.tipe, t.nominal, t.keterangan "
          + "FROM transaksi t "
          + "JOIN kategori k ON t.id_kategori = k.id_kategori "
          + "WHERE t.id_user = ? "
        );
        params.add(idUser);

        if (tipe != null && !tipe.isEmpty()) {
            sql.append("AND k.tipe = ? ");
            params.add(tipe);
        }
        if (bulan > 0) {
            sql.append("AND strftime('%m', t.tanggal) = ? ");
            params.add(String.format("%02d", bulan));
        }
        if (tahun > 0) {
            sql.append("AND strftime('%Y', t.tanggal) = ? ");
            params.add(String.valueOf(tahun));
        }

        sql.append("ORDER BY t.tanggal DESC, t.id_transaksi DESC");

        ResultSet rs = Database.getInstance().executeQuery(sql.toString(), params.toArray());
        while (rs.next()) {
            daftar.add(new TransaksiItem(
                rs.getInt("id_transaksi"),
                rs.getString("tanggal"),
                rs.getString("nama_kategori"),
                rs.getString("tipe"),
                rs.getDouble("nominal"),
                rs.getString("keterangan")
            ));
        }
        return daftar;
    }

    /**
     * Update transaksi yang sudah ada — dipakai tombol "Edit" di Riwayat.
     */
    public static boolean updateTransaksi(int idTransaksi, int idKategori,
                                          String tanggal, double nominal,
                                          String keterangan) throws SQLException {
        String sql  = "UPDATE transaksi "
                    + "SET id_kategori = ?, tanggal = ?, nominal = ?, keterangan = ? "
                    + "WHERE id_transaksi = ?";
        int    rows = Database.getInstance().executeUpdate(
                          sql, idKategori, tanggal, nominal, keterangan, idTransaksi);
        return rows > 0;
    }

    /**
     * Hapus transaksi berdasarkan ID — dipakai tombol "Hapus" di Riwayat.
     */
    public static boolean hapusTransaksi(int idTransaksi) throws SQLException {
        String sql  = "DELETE FROM transaksi WHERE id_transaksi = ?";
        int    rows = Database.getInstance().executeUpdate(sql, idTransaksi);
        return rows > 0;
    }
}
