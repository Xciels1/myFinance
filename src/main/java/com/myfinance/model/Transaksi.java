package com.myfinance.model;

import java.sql.SQLException;

/**
 * Abstract Class Transaksi - Kelas induk untuk Pemasukan dan Pengeluaran.
 *
 * Menerapkan konsep Inheritance:
 *   Pemasukan  --extends--> Transaksi
 *   Pengeluaran --extends--> Transaksi
 *
 * Method abstract harus diimplementasikan oleh kelas turunan.
 */
public abstract class Transaksi {

    // ===================== Atribut Dasar =====================
    protected int    idTransaksi;
    protected String tanggal;     // Format: YYYY-MM-DD
    protected double nominal;
    protected int    idUser;
    protected String keterangan;
    protected int    idKategori;

    // ===================== Constructor =====================
    public Transaksi(int idTransaksi, int idUser, int idKategori,
                     String tanggal, double nominal, String keterangan) {
        this.idTransaksi = idTransaksi;
        this.idUser      = idUser;
        this.idKategori  = idKategori;
        this.tanggal     = tanggal;
        this.nominal     = nominal;
        this.keterangan  = keterangan;
    }

    // Constructor tanpa idTransaksi (untuk data baru yang belum disimpan)
    public Transaksi(int idUser, int idKategori,
                     String tanggal, double nominal, String keterangan) {
        this(0, idUser, idKategori, tanggal, nominal, keterangan);
    }

    // ===================== Getters =====================
    public int    getIdTransaksi() { return idTransaksi; }
    public String getTanggal()     { return tanggal; }
    public double getNominal()     { return nominal; }
    public int    getIdUser()      { return idUser; }
    public String getKeterangan()  { return keterangan; }
    public int    getIdKategori()  { return idKategori; }

    // ===================== Method Abstract =====================

    /**
     * Simpan transaksi ke database.
     * Implementasi ada di kelas Pemasukan dan Pengeluaran.
     */
    public abstract boolean simpanTransaksi() throws SQLException;

    /**
     * Hapus transaksi dari database berdasarkan ID.
     */
    public abstract boolean hapusTransaksi(int idTransaksi) throws SQLException;

    /**
     * Kembalikan detail transaksi dalam format String.
     */
    public abstract String getDetailTransaksi();
}
