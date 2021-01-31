/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.excellentsystem.TokoEmasJagoPusat.Report;

import com.excellentsystem.TokoEmasJagoPusat.DAO.BarangCabangDAO;
import com.excellentsystem.TokoEmasJagoPusat.Function;
import com.excellentsystem.TokoEmasJagoPusat.KoneksiPusat;
import static com.excellentsystem.TokoEmasJagoPusat.Main.rp;
import static com.excellentsystem.TokoEmasJagoPusat.Main.sistem;
import static com.excellentsystem.TokoEmasJagoPusat.Main.tglBarang;
import static com.excellentsystem.TokoEmasJagoPusat.Main.tglNormal;
import com.excellentsystem.TokoEmasJagoPusat.Model.BarangCabang;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

public class LaporanPerformaBarcodeController {

    @SuppressWarnings("rawtypes")
    private JasperPrint jasperPrint;
    @FXML private ImageView imageView;
    @FXML private TextField pageField;
    @FXML private Label totalPageLabel;
    @FXML private Slider zoomLevel;
    private double zoomFactor;
    private double imageHeight;
    private double imageWidth;

    @FXML private DatePicker tglAwalPicker;
    @FXML private DatePicker tglAkhirPicker;
    
    public void initialize(){
        Function.setNumberField(pageField, rp);
        tglAwalPicker.setConverter(Function.getTglConverter());
        tglAwalPicker.setValue(LocalDate.parse(sistem.getTglSystem()));
        tglAwalPicker.setDayCellFactory((final DatePicker datePicker) -> 
                Function.getDateCellMulai(tglAkhirPicker, LocalDate.parse(sistem.getTglSystem())));
        tglAkhirPicker.setConverter(Function.getTglConverter());
        tglAkhirPicker.setValue(LocalDate.parse(sistem.getTglSystem()));
        tglAkhirPicker.setDayCellFactory((final DatePicker datePicker) -> 
                Function.getDateCellAkhir(tglAwalPicker, LocalDate.parse(sistem.getTglSystem())));
    }
    @FXML
    private void lihatLaporan(){
        try(Connection con = KoneksiPusat.getConnection()){
            List<BarangCabang> data = BarangCabangDAO.getAllByTglBarcode(con, tglAwalPicker.getValue().toString(), tglAkhirPicker.getValue().toString());
            Collections.sort(data, (o1, o2) -> {
                int sComp = ((BarangCabang) o1).getInputBy().compareTo(((BarangCabang) o2).getInputBy());
                if (sComp != 0) 
                    return sComp;
                return ((BarangCabang) o1).getAsalBarang().compareTo(((BarangCabang) o2).getAsalBarang());
            });
            JasperDesign jasperDesign = JRXmlLoader.load(getClass().getResourceAsStream("LaporanPerformaBarcode.jrxml"));
            JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(data);
            Map hash = new HashMap();
            hash.put("tanggal", tglNormal.format(tglBarang.parse(tglAwalPicker.getValue().toString()))+" - "+
                        tglNormal.format(tglBarang.parse(tglAkhirPicker.getValue().toString())));
            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
            jasperPrint = JasperFillManager.fillReport(jasperReport,hash, beanColDataSource);
            zoomFactor = 1.5d;
            zoomLevel.setValue(100d);
            imageView.setX(0);
            imageView.setY(0);
            imageHeight = jasperPrint.getPageHeight();
            imageWidth = jasperPrint.getPageWidth();
            zoomLevel.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                zoomFactor = newValue.doubleValue() * 1.5 / 100;
                imageView.setFitHeight(imageHeight * zoomFactor);
                imageView.setFitWidth(imageWidth * zoomFactor);
            });
            if(jasperPrint.getPages().size() > 0){
                showImage(1);
                pageField.setText("1");
                totalPageLabel.setText(String.valueOf(jasperPrint.getPages().size()));
            }else{
                imageView.setImage(null);
                pageField.setText("0");
                totalPageLabel.setText("0");
            }
        }catch(Exception e){
            e.printStackTrace();
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Application error - \n" +e);
            alert.showAndWait();
        }
    }
//    @FXML
//    private void lihatDiagram(){
//        try(Connection con = KoneksiPusat.getConnection()){
//            List<TempTransaksiCabang> data = new ArrayList<>();
//            for(Cabang c : cabang){
//                if(c.isCheck()){
//                    List<TempTransaksiCabang> listTransaksiCabang = TempTransaksiCabangDAO.getAllByDateAndCabangAndSales(con, 
//                            tglAwalPicker.getValue().toString(), tglAkhirPicker.getValue().toString(), c.getKodeCabang(), "%");
//                    for(TempTransaksiCabang t2 : listTransaksiCabang){
//                        boolean status = true;
//                        for(TempTransaksiCabang t : data){
//                            if(t.getTanggal().equals(t2.getTanggal()) && t.getKodeCabang().equals(t2.getKodeCabang())){
//                                t.setQtyPenjualan(t.getQtyPenjualan()+t2.getQtyPenjualan());
//                                t.setTotalPenjualan(t.getTotalPenjualan()+t2.getTotalPenjualan());
//                                t.setQtyPembelian(t.getQtyPembelian()+t2.getQtyPembelian());
//                                t.setTotalPembelian(t.getTotalPembelian()+t2.getTotalPembelian());
//                                t.setQtyTerimaHutang(t.getQtyTerimaHutang()+t2.getQtyTerimaHutang());
//                                t.setTotalTerimaHutang(t.getTotalTerimaHutang()+t2.getTotalTerimaHutang());
//                                t.setQtyHutangLunas(t.getQtyHutangLunas()+t2.getQtyHutangLunas());
//                                t.setTotalHutangLunas(t.getTotalHutangLunas()+t2.getTotalHutangLunas());
//                                t.setTotalHutangBunga(t.getTotalHutangBunga()+t2.getTotalHutangBunga());
//                                status = false;
//                            }
//                        }
//                        if(status)
//                            data.add(t2);
//                    }
//                }
//            }
//            Collections.sort(data, (o1, o2) -> {
//                int sComp = ((TempTransaksiCabang) o1).getTanggal().compareTo(((TempTransaksiCabang) o2).getTanggal());
//                if (sComp != 0) 
//                    return sComp;
//                return ((TempTransaksiCabang) o1).getKodeCabang().compareTo(((TempTransaksiCabang) o2).getKodeCabang());
//            });
//            JasperDesign jasperDesign = JRXmlLoader.load(getClass().getResourceAsStream("LaporanOmzetDiagram.jrxml"));
//            JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(data);
//            Map hash = new HashMap();
//            hash.put("tanggal", tglNormal.format(tglBarang.parse(tglAwalPicker.getValue().toString()))+" - "+
//                        tglNormal.format(tglBarang.parse(tglAkhirPicker.getValue().toString())));
//            JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
//            jasperPrint = JasperFillManager.fillReport(jasperReport,hash, beanColDataSource);
//            zoomFactor = 1.5d;
//            zoomLevel.setValue(100d);
//            imageView.setX(0);
//            imageView.setY(0);
//            imageHeight = jasperPrint.getPageHeight();
//            imageWidth = jasperPrint.getPageWidth();
//            zoomLevel.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
//                zoomFactor = newValue.doubleValue() * 1.5 / 100;
//                imageView.setFitHeight(imageHeight * zoomFactor);
//                imageView.setFitWidth(imageWidth * zoomFactor);
//            });
//            if(jasperPrint.getPages().size() > 0){
//                showImage(1);
//                pageField.setText("1");
//                totalPageLabel.setText(String.valueOf(jasperPrint.getPages().size()));
//            }else{
//                imageView.setImage(null);
//                pageField.setText("0");
//                totalPageLabel.setText("0");
//            }
//        }catch(Exception e){
//            Alert alert = new Alert(Alert.AlertType.ERROR);
//            alert.setTitle("Error");
//            alert.setContentText("Application error - \n" +e);
//            alert.showAndWait();
//        }
//    }
    @FXML
    private void changePage(){
        try {
            int pageNumber = Integer.parseInt(pageField.getText().replaceAll(",", ""));
            if(1<=pageNumber && pageNumber<=jasperPrint.getPages().size()){
                showImage(pageNumber);
                pageField.setText(rp.format(pageNumber));
            }else{
                pageField.setText("0");
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Application error - \n" +e);
            alert.showAndWait();
        }
    }
    @FXML
    public void prevPage(){
        try {
            if(Integer.parseInt(pageField.getText().replaceAll(",", ""))>1){
                int pageNumber = Integer.parseInt(pageField.getText().replaceAll(",", ""))-1;
                showImage(pageNumber);
                pageField.setText(rp.format(pageNumber));
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Application error - \n" +e);
            alert.showAndWait();
        }
    }
    @FXML
    public void nextPage(){
        try {
            if(Integer.parseInt(pageField.getText().replaceAll(",", ""))<jasperPrint.getPages().size()){
                int pageNumber = Integer.parseInt(pageField.getText().replaceAll(",", ""))+1;
                showImage(pageNumber);
                pageField.setText(rp.format(pageNumber));
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Application error - \n" +e);
            alert.showAndWait();
        }
    }
    @FXML
    private void firstPage(){
        try {
            if(jasperPrint.getPages().size() > 0){
                showImage(1);
                pageField.setText("1");
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Application error - \n" +e);
            alert.showAndWait();
        }
    }
    @FXML
    private void lastPage(){
        try {
            if(jasperPrint.getPages().size() > 0){
                showImage(jasperPrint.getPages().size());
                pageField.setText(rp.format(jasperPrint.getPages().size()));
            }
        } catch (Exception e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Application error - \n" +e);
            alert.showAndWait();
        }
    }
    private void showImage(int pageNumber)throws Exception{
        imageView.setFitHeight(imageHeight * zoomFactor);
        imageView.setFitWidth(imageWidth * zoomFactor);
        BufferedImage image = (BufferedImage) JasperPrintManager.printPageToImage(jasperPrint, pageNumber-1, 2);
        WritableImage fxImage = new WritableImage(jasperPrint.getPageWidth(), jasperPrint.getPageHeight());
        imageView.setImage(SwingFXUtils.toFXImage(image, fxImage));
    }
    @FXML
    private void print() {
        try{
            JasperPrintManager.printReport(jasperPrint, true);
        }catch(JRException e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Application error - \n" +e);
            alert.showAndWait();
        }
    }
    
    
}
