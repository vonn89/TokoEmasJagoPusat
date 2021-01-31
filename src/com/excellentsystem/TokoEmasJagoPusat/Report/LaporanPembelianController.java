/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.excellentsystem.TokoEmasJagoPusat.Report;

import com.excellentsystem.TokoEmasJagoPusat.DAO.AmbilBarangDetailDAO;
import com.excellentsystem.TokoEmasJagoPusat.DAO.CabangDAO;
import com.excellentsystem.TokoEmasJagoPusat.Function;
import com.excellentsystem.TokoEmasJagoPusat.KoneksiPusat;
import static com.excellentsystem.TokoEmasJagoPusat.Main.gr;
import static com.excellentsystem.TokoEmasJagoPusat.Main.rp;
import static com.excellentsystem.TokoEmasJagoPusat.Main.sistem;
import static com.excellentsystem.TokoEmasJagoPusat.Main.tglBarang;
import static com.excellentsystem.TokoEmasJagoPusat.Main.tglLengkap;
import static com.excellentsystem.TokoEmasJagoPusat.Main.tglNormal;
import static com.excellentsystem.TokoEmasJagoPusat.Main.tglSql;
import com.excellentsystem.TokoEmasJagoPusat.Model.AmbilBarangDetail;
import com.excellentsystem.TokoEmasJagoPusat.Model.Cabang;
import java.awt.image.BufferedImage;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
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

public class LaporanPembelianController {

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
    @FXML private ComboBox<String> cabangCombo;
    @FXML private ComboBox<String> groupByCombo;
    @FXML private TextField searchField;
    private ObservableList<String> group = FXCollections.observableArrayList();
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
        groupByCombo.setItems(group);
        group.add("No Pembelian");
        group.add("Kategori");
        group.add("Barang");
        groupByCombo.getSelectionModel().selectFirst();
        getCabang();
        getBarang();
    }
    private void getCabang(){
        try(Connection conPusat = KoneksiPusat.getConnection()){
            ObservableList<String> cabang = FXCollections.observableArrayList();
            List<Cabang> listCabang = CabangDAO.getAll(conPusat);
            for(Cabang c: listCabang){
                cabang.add(c.getKodeCabang());
            }
            cabangCombo.setItems(cabang);
            cabangCombo.getSelectionModel().selectFirst();
        }catch(Exception e){
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Application error - \n" +e);
            alert.showAndWait();
        }
    }   
    @FXML
    private void getBarang(){
        if(cabangCombo.getSelectionModel().getSelectedItem()!=null){
            try(Connection conPusat = KoneksiPusat.getConnection()){
                if(groupByCombo.getSelectionModel().getSelectedItem().equals("No Pembelian")){
                    List<AmbilBarangDetail> listPembelianDetail = AmbilBarangDetailDAO.getAllByTglPembelianAndCabang(
                        conPusat, tglAwalPicker.getValue().toString(), tglAkhirPicker.getValue().toString(), cabangCombo.getSelectionModel().getSelectedItem());
                    List<AmbilBarangDetail> filterData = filterData(listPembelianDetail);
        //            listBarang.sort(Comparator.comparing(Barang::getKodeKategori));
                    Collections.sort(filterData, (o1, o2) -> {
                        int sComp = ((AmbilBarangDetail) o1).getNoPembelian().compareTo(((AmbilBarangDetail) o2).getNoPembelian());
                        if (sComp != 0) 
                            return sComp;
                        return ((AmbilBarangDetail) o1).getKodeJenis().compareTo(((AmbilBarangDetail) o2).getKodeJenis());
                    });
                    JasperDesign jasperDesign = JRXmlLoader.load(getClass().getResourceAsStream("LaporanPembelianNoPembelian.jrxml"));
                    JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(filterData);
                    Map hash = new HashMap();
                    hash.put("kodeCabang", cabangCombo.getSelectionModel().getSelectedItem());
                    hash.put("tanggal", tglNormal.format(tglBarang.parse(tglAwalPicker.getValue().toString()))+" - "+
                            tglNormal.format(tglBarang.parse(tglAkhirPicker.getValue().toString())));
                    JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
                    jasperPrint = JasperFillManager.fillReport(jasperReport,hash, beanColDataSource);
                }else if(groupByCombo.getSelectionModel().getSelectedItem().equals("Kategori")){
                    List<AmbilBarangDetail> listPembelianDetail = AmbilBarangDetailDAO.getAllByTglPembelianAndCabang(
                        conPusat, tglAwalPicker.getValue().toString(), tglAkhirPicker.getValue().toString(), cabangCombo.getSelectionModel().getSelectedItem());
                    List<AmbilBarangDetail> filterData = filterData(listPembelianDetail);
        //            listBarang.sort(Comparator.comparing(Barang::getKodeKategori));
                    Collections.sort(filterData, (o1, o2) -> {
                        int sComp = ((AmbilBarangDetail) o1).getKodeKategori().compareTo(((AmbilBarangDetail) o2).getKodeKategori());
                        if (sComp != 0) 
                            return sComp;
                        return ((AmbilBarangDetail) o1).getKodeJenis().compareTo(((AmbilBarangDetail) o2).getKodeJenis());
                    });
                    JasperDesign jasperDesign = JRXmlLoader.load(getClass().getResourceAsStream("LaporanPembelianKategori.jrxml"));
                    JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(filterData);
                    Map hash = new HashMap();
                    hash.put("kodeCabang", cabangCombo.getSelectionModel().getSelectedItem());
                    hash.put("tanggal", tglNormal.format(tglBarang.parse(tglAwalPicker.getValue().toString()))+" - "+
                            tglNormal.format(tglBarang.parse(tglAkhirPicker.getValue().toString())));
                    JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
                    jasperPrint = JasperFillManager.fillReport(jasperReport, hash, beanColDataSource);
                }else if(groupByCombo.getSelectionModel().getSelectedItem().equals("Barang")){
                    List<AmbilBarangDetail> listPembelianDetail = AmbilBarangDetailDAO.getAllByTglPembelianAndCabang(
                        conPusat, tglAwalPicker.getValue().toString(), tglAkhirPicker.getValue().toString(), cabangCombo.getSelectionModel().getSelectedItem());
                    List<AmbilBarangDetail> filterData = filterData(listPembelianDetail);
        //            listBarang.sort(Comparator.comparing(Barang::getKodeKategori));
                    Collections.sort(filterData, (o1, o2) -> {
                        int sComp = ((AmbilBarangDetail) o1).getKodeKategori().compareTo(((AmbilBarangDetail) o2).getKodeKategori());
                        if (sComp != 0) 
                            return sComp;
                        return ((AmbilBarangDetail) o1).getKodeJenis().compareTo(((AmbilBarangDetail) o2).getKodeJenis());
                    });
                    JasperDesign jasperDesign = JRXmlLoader.load(getClass().getResourceAsStream("LaporanPembelianBarang.jrxml"));
                    JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(filterData);
                    Map hash = new HashMap();
                    hash.put("kodeCabang", cabangCombo.getSelectionModel().getSelectedItem());
                    hash.put("tanggal", tglNormal.format(tglBarang.parse(tglAwalPicker.getValue().toString()))+" - "+
                            tglNormal.format(tglBarang.parse(tglAkhirPicker.getValue().toString())));
                    JasperReport jasperReport = JasperCompileManager.compileReport(jasperDesign);
                    jasperPrint = JasperFillManager.fillReport(jasperReport, hash, beanColDataSource);
                }
                zoomFactor = 1.5d;
                zoomLevel.setValue(100d);
                imageView.setX(0);
                imageView.setY(0);
                imageHeight = jasperPrint.getPageHeight();
                imageWidth = jasperPrint.getPageWidth();
                zoomLevel.valueProperty().addListener((ObservableValue<? extends Number> observable, Number oldValue, Number newValue) -> {
                    zoomFactor = newValue.doubleValue() / 100 *1.5;
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
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Error");
                alert.setContentText("Application error - \n" +e);
                alert.showAndWait();
            }
        }
    }
    private Boolean checkColumn(String column){
        if(column!=null){
            if(column.toLowerCase().contains(searchField.getText().toLowerCase()))
                return true;
        }
        return false;
    }
    private List<AmbilBarangDetail> filterData(List<AmbilBarangDetail> listData) throws Exception{
        List<AmbilBarangDetail> filterData = new ArrayList<>();
        for (AmbilBarangDetail p : listData) {
            if (searchField.getText() == null || searchField.getText().equals(""))
                filterData.add(p);
            else{
                if(checkColumn(p.getNoPembelian())||
                    checkColumn(tglLengkap.format(tglSql.parse(p.getTglPembelian())))||
                    checkColumn(p.getKodeSales())||
                    checkColumn(p.getNamaBarang())||
                    checkColumn(p.getKodeJenis())||
                    checkColumn(p.getKodeKategori())||
                    checkColumn(gr.format(p.getBeratKotor()))||
                    checkColumn(gr.format(p.getBeratBersih()))||
                    checkColumn(gr.format(p.getBeratPersen()))||
                    checkColumn(gr.format(p.getPersentaseEmas()))||
                    checkColumn(rp.format(p.getHargaBeli()))||
                    checkColumn(rp.format(p.getHargaKomp()))||
                    checkColumn(rp.format(p.getQty())))
                    filterData.add(p);
            }
        }
        return filterData;
    }
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
        try {
            JasperPrintManager.printReport(jasperPrint, true);
        } catch (JRException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setContentText("Application error - \n" +e);
            alert.showAndWait();
        }
    }
    
    
}
