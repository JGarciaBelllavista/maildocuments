/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package maildocuments;

import abbacinoutils.Config;
import abbacinoutils.LogSeyma;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Blob;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author lorenzo
 */
public class DocWriter extends Document {

    private String mailtype;
    private String DocNum;
    private String DocNumUSA = "";
    private String DocDate;
    private String CardCode;
    private float Doctotal;
    private float VatSum;
    private String DiscSum;
    private String DocCur;
    private String TotalExpns;
    private String DiscPrcnt;
    private String DirFiscal;
    private String DirFactura;
    private String DatosPago;
    private String SlpName;
    private String Comments;
    private String CompnyName;
    private String CompnyNIF;
    private String BankCode;
    private String BankName;
    private String idioma;
    private String Street;
    private String City;
    private ArrayList<String> fechavenciments = new ArrayList<>();
    private ArrayList<String> importevenciments = new ArrayList<>();
    private int nvenciments = 0;
    private ArrayList<String> itemcodes = new ArrayList<>();
    private ArrayList<String> itemDesc = new ArrayList<>();
    private ArrayList<String> itemquant = new ArrayList<>();
    private ArrayList<String> itemprice = new ArrayList<>();
    private ArrayList<String> itemtotal = new ArrayList<>();
    private ArrayList<String> intrastatpartida = new ArrayList<>();
    private ArrayList<String> intrastatcountry = new ArrayList<>();
    private ArrayList<Float> intrastatquantity = new ArrayList<>();
    private ArrayList<Float> intrastatvalue = new ArrayList<>();
    private ArrayList<Float> itemdiscount = new ArrayList<>();
    private ArrayList<Float> itemtarifa = new ArrayList<>();
    private ArrayList<Float> itempvpr = new ArrayList<>();
    private ArrayList<String> itemiva = new ArrayList<>();
    private ArrayList<String> itemtreetype = new ArrayList<>();
    private ArrayList<Blob> itemimg = new ArrayList<>();
    private int nitems = 0;
    private int nintrastat = 0;
    private int npagina;
    private int totalnpagina;
    private float qtotal = 0;
    private float ptotal = 0;
    private float iva;
    private float re;
    private float sumre;
    private boolean dtoarticles = false;
    private HashMap<String, String> namesES = new HashMap<>();
    private HashMap<String, String> namesEN = new HashMap<>();

    BaseFont calibri;
    private Font h1;
    private Font h2b;
    private Font h3b;
    private Font fb;
    private Font f;
    private Font fcg;
    private Font fal;

    public  DocWriter(ResultSet rowdata, String i_mailtype, boolean isSeyma, int docnumusa,String comments,String usadocdate) throws ParseException, SQLException, DocumentException, IOException {
        mailtype = i_mailtype;        
        iniLangMap();
        calibri = BaseFont.createFont(Config.param(mailtype, ConfigStr.DOC_FONT), BaseFont.WINANSI, true);
        h1 = new Font(calibri, 16, Font.BOLD);
        h2b = new Font(calibri, 14, Font.BOLD);
        h3b = new Font(calibri, 10, Font.BOLD);
        fb = new Font(calibri, 8, Font.BOLD);
        f = new Font(calibri, 8, Font.NORMAL);
        fcg = new Font(calibri, 8, Font.NORMAL, new BaseColor(160, 160, 160));
        fal = new Font(calibri, 5, Font.NORMAL);
        DocNum = rowdata.getString(ConfigStr.DOC_NUM);
        DocDate = rowdata.getString(ConfigStr.DOC_DATE);
        if (mailtype.equals(ConfigStr.FACTURASUSA)) {
            DocNumUSA = String.valueOf(docnumusa);
            DocDate = usadocdate;
        }
        CardCode = rowdata.getString(ConfigStr.CARD_CODE);
        Doctotal = rowdata.getFloat(ConfigStr.DOC_TOTAL);
        VatSum = rowdata.getFloat(ConfigStr.VAT_SUM);
        DiscSum = roundNum(rowdata.getFloat(ConfigStr.DISC_SUM),mailtype);
        DocCur = rowdata.getString(ConfigStr.DOC_CUR);
        TotalExpns = (rowdata.getString(ConfigStr.TOTAL_EXPNS)).replace(".",",");
        DiscPrcnt = roundNum(rowdata.getFloat(ConfigStr.DISC_PRCNT),mailtype)  + '%';
        String cardname = rowdata.getString(ConfigStr.CARD_NAME);
        String cardfname = rowdata.getString(ConfigStr.CARD_FNAME);
        
        this.DirFactura = "";
        if (cardname != null && !cardname.equals("")) {
            DirFactura = cardname;
        }
        if (cardfname != null && !cardfname.equals("") && !cardfname.equals(".") && !cardfname.equals(DirFactura)) {
            DirFactura += '\n' + cardfname;
        }
        DirFactura += "\n\n" + rowdata.getString(ConfigStr.ADDRESS2);
        if (mailtype.equals(ConfigStr.FACTURASUSA) || mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            DirFactura = DirFactura.replaceAll("EE.UU.", "U.S.A.");
            DirFiscal = DirFactura;            
        } else {
            DirFiscal = "";
            if (cardname != null && !cardname.equals("")) {
                DirFiscal = cardname;
            } else if (cardfname != null && !cardfname.equals("") && !isSeyma) {
                DirFiscal = cardfname;
            }
            DirFiscal += "\n\n" + rowdata.getString(ConfigStr.ADDRESS);        
            if (rowdata.getString(ConfigStr.LIC_TRAD_NUM) != null && !rowdata.getString(ConfigStr.LIC_TRAD_NUM).equals("") 
                    && !CardCode.substring(0, 2).equals("CW")) {
                DirFiscal += '\n' + rowdata.getString(ConfigStr.LIC_TRAD_NUM);
            }
        }

        idioma = rowdata.getString(ConfigStr.COUNTRY);
        String datospagoStr = getColumnNamePerLangauge(idioma, "PAYMVAL");
        if (mailtype.equals(ConfigStr.FACTURASUSA)) {
            datospagoStr = "Payment Method: Due and Payable Upon Receipt\n";
        } else if (mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            datospagoStr = "Payment Method: Credit Card\nPayment Terms: Due and payable upon receipt\n";
        }
        
        DatosPago = datospagoStr.replace("&1&", rowdata.getString(ConfigStr.PEY_METHOD))
                                     .replace("&2&", rowdata.getString(ConfigStr.PYMNT_GROUP));
        if (rowdata.getString(ConfigStr.CARD_CODE).substring(0, 2).equals("CW"))
            DatosPago = DatosPago.split("\n")[0];
        SlpName = rowdata.getString(ConfigStr.SLP_NAME);
        Comments = "";
        if (mailtype.equals(ConfigStr.ABONOS) && rowdata.getString(ConfigStr.DESCR) != null) {
            Comments = rowdata.getString(ConfigStr.DESCR) + ".";
        }
        if (mailtype.equals(ConfigStr.FACTURASUSA) || mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            Comments += comments;
        } else if (!(mailtype.equals(ConfigStr.FACTURAS) && rowdata.getString(ConfigStr.CARD_CODE).substring(0, 2).equals("CW")) 
                && rowdata.getString(ConfigStr.COMMENT) != null) {
            Comments += "\n" + rowdata.getString(ConfigStr.COMMENT);
        }
        CompnyName = rowdata.getString(ConfigStr.COMPNY_NAME);
        BankCode = rowdata.getString(ConfigStr.BANK_CODE);
        BankName = rowdata.getString(ConfigStr.BANK_NAME);
        CompnyNIF = rowdata.getString(ConfigStr.TAX_ID_NUM);
        if (mailtype.equals(ConfigStr.ABONOS)) {
            Street = rowdata.getString(ConfigStr.STREET);
            City = rowdata.getString(ConfigStr.CITY);
        }
        iva = rowdata.getFloat(ConfigStr.RATE);
        re = rowdata.getFloat(ConfigStr.EQU_VAT_PR);
        sumre = rowdata.getFloat(ConfigStr.EQU_VAT_SUM);
    }

    public void addInterState(ResultSet rsintrastat) throws SQLException {
        if (rsintrastat.getString(ConfigStr.U_SEIPARTIDA) == null) {
                intrastatpartida.add("");
        } else {
            intrastatpartida.add(rsintrastat.getString(ConfigStr.U_SEIPARTIDA));
        }
        if (rsintrastat.getString(ConfigStr.OCRY_NAME) == null) {
            intrastatcountry.add("");
        } else {
            intrastatcountry.add(rsintrastat.getString(ConfigStr.OCRY_NAME));
        }
        intrastatquantity.add(rsintrastat.getFloat(ConfigStr.QUANTITY_SUM));
        intrastatvalue.add(rsintrastat.getFloat(ConfigStr.LINETOTAL_SUM));
        nintrastat++;
    }

    public void addDueDate(String fecha, float importe) throws ParseException {
        fechavenciments.add(fecha);
        importevenciments.add(roundNum(importe,mailtype));
        nvenciments++;

    }

    public void addItem(String code, ResultSet rsarticles) throws SQLException {
        itemcodes.add(code);
        itemDesc.add(rsarticles.getString(ConfigStr.DSCRIPTION));
        itemquant.add(String.valueOf(rsarticles.getInt(ConfigStr.QUANTITY)));
        itemprice.add(roundNum(rsarticles.getFloat(ConfigStr.PRICE_O),mailtype));
        if (!mailtype.equals(ConfigStr.PEDIDOSUSA))
            itemiva.add(String.valueOf(rsarticles.getInt(ConfigStr.VAT_PRCNT)) + "%");
        else
            itemiva.add("");
        itemtotal.add(roundNum(rsarticles.getFloat(ConfigStr.LINE_TOTAL),mailtype));
        itempvpr.add(rsarticles.getFloat(ConfigStr.PRICE_1));
        itemtreetype.add(rsarticles.getString(ConfigStr.TREE_TYPE));
        if (mailtype.equals(ConfigStr.PEDIDOS) || mailtype.equals(ConfigStr.PEDIDOSUSA))
            itemimg.add(rsarticles.getBlob(ConfigStr.IMAGE));
        itemdiscount.add(rsarticles.getFloat(ConfigStr.DISC_PRCNT));
        if (rsarticles.getFloat(ConfigStr.DISC_PRCNT) > 0) { //Si hi ha qualque descompte hem de mostrar la columna de descomptes
            dtoarticles = true;
        }
        itemtarifa.add(rsarticles.getFloat(ConfigStr.PRICE_BEF_DI));
        nitems++;
        qtotal += rsarticles.getFloat(ConfigStr.QUANTITY);
        ptotal += rsarticles.getFloat(ConfigStr.LINE_TOTAL);
    }

    private String formatoFloat(float n) {
        String nstr = String.format("%,.1f", n);
        if (!mailtype.equals(ConfigStr.FACTURASUSA) &&  !mailtype.equals(ConfigStr.PEDIDOSUSA) && nstr.charAt(nstr.length() - 2) != ',')
            nstr = nstr.replace(".",";").replace(",",".").replace(";",",");
        return nstr;
    }
    
    public static  String roundNum(double numero, String decimales, String imailtype) {
        String numberstr = String.format("%,." + decimales + "f", new BigDecimal(numero));
        if (!decimales.equals("0") && !imailtype.equals(ConfigStr.FACTURASUSA) && !imailtype.equals(ConfigStr.PEDIDOSUSA) 
                && numberstr.charAt(numberstr.length() - Integer.valueOf(decimales) - 1) != ',')
            numberstr = numberstr.replace(".",";").replace(",",".").replace(";",",");
        return numberstr;
    }
    
    public static String roundNum(double numero, String imailtype) {
        return roundNum(numero,"2",imailtype);
    }
    
    private int lineasCorreccionIntrastat() {
        int val = Integer.valueOf(Config.param(mailtype,ConfigStr.DOC_MAXLINENUM_LAST));
        int result = 0;
        
        if (!idioma.equals("ES")) {
            if (intrastatTienePaginaPropia()) {
                result = val - (nintrastat * 2);
            } else {
                result = val - ((nitems % val) + (nintrastat * 3) + 5);
            }
        }
        return result;
    }

    private boolean intrastatTienePaginaPropia() {
        boolean result = false;
        double maxlinesperpage = Double.valueOf(Config.param(mailtype,ConfigStr.DOC_MAXLINENUM));
        double maxlinesperpagelast = Double.valueOf(Config.param(mailtype,ConfigStr.DOC_MAXLINENUM_LAST));
        double totallines = nitems;  //+3 for comments
        if (mailtype.equals(ConfigStr.ABONOS) || mailtype.equals(ConfigStr.FACTURAS)) 
            totallines += nintrastat * 3; // + nvenciments;
        if ((mailtype.equals(ConfigStr.ABONOS) || mailtype.equals(ConfigStr.FACTURASUSA)
                || mailtype.equals(ConfigStr.FACTURAS)) && !idioma.equals("ES")) {
            result = ((totallines % maxlinesperpage) + (nintrastat * 3) + 5) >= maxlinesperpage;
        }
        return result;
    }

    private int numeroDePaginas() {
        int npaginas = 0;
        double maxlinesperpage = Double.valueOf(Config.param(mailtype,ConfigStr.DOC_MAXLINENUM));
        int maxlineslastpage = Integer.valueOf(Config.param(mailtype,ConfigStr.DOC_MAXLINENUM_LAST));
        double totallines = nitems + 3; //+3 for comments
        if (nitems + nvenciments < maxlinesperpage && nitems + nvenciments > maxlineslastpage) {
            npaginas = 1;
        }
        if ((mailtype.equals(ConfigStr.ABONOS) || mailtype.equals(ConfigStr.FACTURASUSA) 
                || mailtype.equals(ConfigStr.FACTURAS)) && !idioma.equals("ES")) 
            totallines += nintrastat * 3; // + nvenciments;
        if (mailtype.equals(ConfigStr.FACTURAS)) 
            totallines += nvenciments;
        npaginas += (int) Math.ceil(totallines/maxlinesperpage);
        if (intrastatTienePaginaPropia()) {
            npaginas++;
        }
        return npaginas;
    }

    private int numeroArticulosPorPagina() {
        double totallines = nitems + 3; //+3 for comments
        if ((mailtype.equals(ConfigStr.ABONOS) || mailtype.equals(ConfigStr.FACTURASUSA)
                || mailtype.equals(ConfigStr.FACTURAS)) 
                && !idioma.equals("ES")) 
            totallines += nintrastat  * 3;
        if (mailtype.equals(ConfigStr.FACTURAS)) 
            totallines += nvenciments;
        LogSeyma.printdebug("total lines: " + totallines);
        int maxlinesperpage = Integer.valueOf(Config.param(mailtype,ConfigStr.DOC_MAXLINENUM));
        int maxlineslastpage = Integer.valueOf(Config.param(mailtype,ConfigStr.DOC_MAXLINENUM_LAST));
        int result;

        if (npagina == 1 && totalnpagina == 1 && mailtype.equals(ConfigStr.FACTURAS)) {
            result = maxlineslastpage + 1;
        } else if (totallines <= maxlinesperpage && !mailtype.equals(ConfigStr.PEDIDOS) 
                || ((npagina == totalnpagina) && (idioma.equals("ES")))) {
            result = maxlineslastpage;
        } else if (totallines <= maxlinesperpage && mailtype.equals(ConfigStr.PEDIDOS)) {
            result = maxlinesperpage - 1;
        } else if (npagina == 1 && totalnpagina > 1 && 
                    (mailtype.equals(ConfigStr.ABONOS) || mailtype.equals(ConfigStr.FACTURASUSA))) {
            result = maxlinesperpage - 8;
        } else {
            result = maxlinesperpage;
        }
        
        /*if (npagina == totalnpagina)
            result -= nvenciments;*/
        return result;
    }

    public void generateDocument(String docnumber) throws SQLException {
        FileOutputStream ficheroPdf;
        String[] filenames = Config.param(mailtype, ConfigStr.DOC_FILENAMES).split(Config.param(Config.FILE_SPR));
        int icompany = 0;
        if (CompnyNIF.equals("ESB07914732")) { //Mediterrani Bags S.L.U. 
                icompany = 1;
        }
        try {
            ficheroPdf = new FileOutputStream(Config.param(mailtype, ConfigStr.DOC_FOLDER) + filenames[icompany] + docnumber + 
                                              Config.param(ConfigStr.DOC_FILEEXT));
            PdfWriter.getInstance(this, ficheroPdf).setInitialLeading(20);
        } catch (FileNotFoundException | DocumentException ex) {
            LogSeyma.println("Exception: " + ex.toString());
        }
        
        try {
            this.open();
            int i = 0;
            npagina = 1;
            totalnpagina = numeroDePaginas();
            for (int k = 0; k < totalnpagina; k++) {                 
                generateDocHeader();
                generateCompanyData();
                generateAddressData();
                generatePayData();
                generateRepreData();                            
                i = genereateItemsList(i);

                if ((mailtype.equals(ConfigStr.ABONOS) || mailtype.equals(ConfigStr.FACTURAS)) && npagina == totalnpagina && !idioma.equals("ES")) {
                    generateCorreccionInterstat();
                }

                if ((npagina == totalnpagina && !intrastatTienePaginaPropia()) || (npagina == (totalnpagina - 1) && intrastatTienePaginaPropia())) {
                    generateDocTotal();
                    generateDocComments();
                }

                if ((mailtype.equals(ConfigStr.ABONOS) || mailtype.equals(ConfigStr.FACTURAS)) && npagina == totalnpagina && !idioma.equals("ES")) {
                    generateInterstat();
                }

                //Si es la última página imprimimos el texto de aviso legal.
                if (npagina == totalnpagina) {
                    generateDocumentFooter(icompany);
                }
                generateLogo(icompany);
                this.newPage();
                npagina++;
            }

            this.close();
        } catch (DocumentException | IOException ex) {
            LogSeyma.println("Exception: " + ex.toString());
        }
    }

    private void generateDocumentFooter(int icompany) throws DocumentException, BadElementException, IOException {
        //Párrafo que contiene el texto de Aviso Legal.
        Paragraph avisoLegal;
        String avisolegalstr;
        String nif = CompnyNIF.substring(2);
        String[] companies = Config.param(ConfigStr.COMPANIES).split(Config.param(Config.FILE_SPR));
        if ((idioma.equals("ES") || idioma.equals("AD")) && !mailtype.equals(ConfigStr.FACTURASUSA)  && !mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            avisolegalstr = Config.param(mailtype,ConfigStr.AVISO_LEGAL_ES);
        } else {
            avisolegalstr = Config.param(mailtype,ConfigStr.AVISO_LEGAL_EN);
        }
        avisoLegal = new Paragraph(avisolegalstr.replace("&NIF&",nif).replace(Config.param(ConfigStr.COMPANY_RPL),companies[icompany]), fal);
        avisoLegal.setAlignment(Element.ALIGN_JUSTIFIED);
        avisoLegal.setSpacingAfter(8);
        avisoLegal.setSpacingBefore(8);
        this.add(avisoLegal);
    }

    private void generateLogo(int icompany) throws DocumentException, IOException, NumberFormatException {
        //Párrafo que contiene el texto de Asegurada por SolUnión.
        if (!mailtype.equals(ConfigStr.FACTURASUSA) && !mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            Paragraph solUnion = new Paragraph(getColumnNamePerLangauge(idioma,"SOLUN"), fb); //SOLUN
            solUnion.setAlignment(Element.ALIGN_CENTER);
            solUnion.setSpacingAfter(0);
            solUnion.setSpacingBefore(5);
            this.add(solUnion);

            Image logo = null;
            String[] logos = Config.param(ConfigStr.LOGO_PATHS).split(Config.param(Config.FILE_SPR));
            logo = Image.getInstance(logos[icompany]);
            int logopercent = Integer.valueOf(Config.param(ConfigStr.LOGO_SIZE_PERCENT));
            logo.scalePercent(logopercent);
            logo.setAlignment(Element.ALIGN_CENTER);
            this.add(logo);
            
            //Párrafo que contiene el texto de Registro Mercantil.                 
            Paragraph regMecantil;
            regMecantil = new Paragraph(Config.param(ConfigStr.MECANTIL),fb);
            regMecantil.setAlignment(Element.ALIGN_CENTER);
            regMecantil.setSpacingAfter(0);
            regMecantil.setSpacingBefore(0);
            this.add(regMecantil);
        }
    }

    private void generateCorreccionInterstat() throws DocumentException {
        PdfPTable tablaCorreccionIntrastat = new PdfPTable(1);
        tablaCorreccionIntrastat.setWidthPercentage(100);
        int numlinescorrection = lineasCorreccionIntrastat();
        for (int j = 0; j < numlinescorrection; j++) {
            addPropertyCell(" ", tablaCorreccionIntrastat, f);
        }
        this.add(tablaCorreccionIntrastat);
    }
    
    private void generateInterstat() throws DocumentException {
        //Tabla que contiene los datos de intrastat
        PdfPTable tablaIntrastat = new PdfPTable(1);
        tablaIntrastat.setWidthPercentage(100);
        tablaIntrastat.setSpacingBefore(5);
        PdfPCell cintrastat1 = new PdfPCell(new Paragraph("INTRASTAT STATISTICS CODES", h2b));
        cintrastat1.setBorder(Rectangle.BOX);
        cintrastat1.setBorderColor(BaseColor.LIGHT_GRAY);
        cintrastat1.setBorderColorBottom(BaseColor.WHITE);
        cintrastat1.setBorderWidthBottom(0);
        cintrastat1.setHorizontalAlignment(Element.ALIGN_CENTER);
        PdfPTable tintaux = new PdfPTable(3);
        String[] instrastatstrs;
        HashMap<String,float[]> instratmap = new HashMap<>();
        String partidaprevia = "";
        for (int j = 0; j < nintrastat; j++) {
            if (!partidaprevia.equals(intrastatpartida.get(j))) { //hay que traducir los textos.
                float quantity = 0;
                float value = 0;
                for (int m = j; m < nintrastat && intrastatpartida.get(j).equals(intrastatpartida.get(m)); m++) {
                    quantity += intrastatquantity.get(m);
                    value += intrastatvalue.get(m);
                }
                instrastatstrs = new String[]{"Taric: " + intrastatpartida.get(j),
                                              "Quantity: " + roundNum(quantity,"0",mailtype),
                                              "Value: " + roundNum(value,mailtype)};
                addIntrastatCells(instrastatstrs, tintaux,fb);
                partidaprevia = intrastatpartida.get(j);
            }
            instrastatstrs = new String[]{"Made in " + intrastatcountry.get(j),
                                          "Quantity: " + roundNum(intrastatquantity.get(j),"0",mailtype),
                                          "Value: " + roundNum(intrastatvalue.get(j),mailtype)};
            addIntrastatCells(instrastatstrs, tintaux, f);
            
            if (!instratmap.containsKey(intrastatcountry.get(j))) {
                instratmap.put(intrastatcountry.get(j), new float[]{intrastatquantity.get(j),intrastatvalue.get(j)});
            } else {
                instratmap.get(intrastatcountry.get(j))[0] += intrastatquantity.get(j);
                instratmap.get(intrastatcountry.get(j))[1] += intrastatvalue.get(j);
            }
        }
        PdfPCell cintrastat2 = new PdfPCell(tintaux);
        cintrastat2.setBorder(Rectangle.BOX);
        cintrastat2.setBorderColor(BaseColor.LIGHT_GRAY);
        cintrastat2.setBorderColorTop(BaseColor.WHITE);
        cintrastat2.setBorderWidthTop(0);
        tablaIntrastat.addCell(cintrastat1);
        tablaIntrastat.addCell(cintrastat2);
        this.add(tablaIntrastat);
        PdfPTable tablaIntrastat2 = new PdfPTable(1);
        tablaIntrastat2.setWidthPercentage(100);
        tablaIntrastat2.setSpacingBefore(5);
        PdfPTable tauxint2 = new PdfPTable(3);
        
        for (Map.Entry instracountry : instratmap.entrySet()) {
           String country = (String) instracountry.getKey();
            float[] instvals = (float[]) instracountry.getValue();
            
            if (instvals[0] != 0) {
            instrastatstrs = new String[]{"Total made in: " + country,
                                          "Quantity: " + roundNum(instvals[0],"0",mailtype),
                                          "Value: " + roundNum(instvals[1],mailtype)};
            addIntrastatCells(instrastatstrs, tauxint2, fb);
            }
        }
        PdfPCell cintborder = new PdfPCell(tauxint2);
        cintborder.setBorder(Rectangle.BOX);
        cintborder.setBorderColor(BaseColor.LIGHT_GRAY);
        tablaIntrastat2.addCell(cintborder);
        this.add(tablaIntrastat2);
    }

    private void addPropertyCell(String propstr, PdfPTable pdftable, Font ifont) {
        addPropertyCell(propstr, pdftable, ifont, -1);
    }

    private void addPropertyCell(String propstr, PdfPTable pdftable, Font ifont, int halightment) {
        addPropertyCell(propstr, pdftable, ifont, halightment,-1);
    }
    private void addPropertyCell(String propstr, PdfPTable pdftable, Font ifont, int halightment, int valightment) {
        addPropertiesCells(new String[]{propstr}, pdftable, ifont, halightment, valightment);
    }

    private void addPropertiesCells(String[] propstrs, PdfPTable pdftable, Font ifont) {
        addPropertiesCells(propstrs, pdftable, ifont, -1);
    }    
    
    private void addPropertiesCells(String[] propstrs, PdfPTable pdftable, Font ifont, int halightment) {
        addPropertiesCells(propstrs , pdftable, ifont, halightment, -1);
    }
    
    private void addPropertiesCells(String[] propstrs, PdfPTable pdftable, Font ifont, int halightment, int valightment) {
        PdfPCell pdfcell;
        for (String instrastatstr : propstrs) {
            pdfcell = new PdfPCell(new Paragraph(instrastatstr, ifont));
            pdfcell.setBorder(Rectangle.NO_BORDER);
            if (halightment > -1)
                pdfcell.setHorizontalAlignment(halightment);
            if (valightment > -1)
                pdfcell.setVerticalAlignment(valightment);
            pdftable.addCell(pdfcell);
        }
    }

    private void addIntrastatCells(String[] instrastatstrs, PdfPTable tintaux, Font ifont) {
        addPropertiesCells(instrastatstrs, tintaux, ifont, Element.ALIGN_CENTER);
    }

    private void generateDocComments() throws DocumentException {
        PdfPTable comentarios = new PdfPTable(8);
        comentarios.setWidthPercentage(100);
        comentarios.setSpacingBefore(5);
        ArrayList<String> columnNames = new ArrayList<>();
        if (mailtype.equals(ConfigStr.FACTURAS)) {
            columnNames.add("DUEDATE");
        } else if (!mailtype.equals(ConfigStr.ABONOS) && !mailtype.equals(ConfigStr.FACTURASUSA)) {
            columnNames.add("DDELIVERY");
        }
        
        if (!mailtype.equals(ConfigStr.ABONOS) && !mailtype.equals(ConfigStr.FACTURASUSA)) {
            columnNames.add("SUM");
            columnNames.add("COMMENT");
        }        
        for (String columnname : columnNames) {
            PdfPCell comments = new PdfPCell(new Paragraph(getColumnNamePerLangauge(idioma, columnname),fb));
            if (columnNames.indexOf(columnname) == columnNames.size() - 1) {
                comments.setBorder(Rectangle.NO_BORDER);
                comments.setIndent(20);
                comments.setColspan(6);
            } else {                
                comments.setBorder(Rectangle.BOX);
                comments.setBorderColor(BaseColor.LIGHT_GRAY);
                comments.setBorderColorLeft(BaseColor.WHITE);
                comments.setBorderColorRight(BaseColor.WHITE);
                comments.setBorderWidthLeft(0);
                comments.setBorderWidthRight(0);
            }
            comentarios.addCell(comments);
        }
        String comment;
        if (!mailtype.equals(ConfigStr.ABONOS) && !mailtype.equals(ConfigStr.FACTURASUSA)) {
            for(int j = 0; j < nvenciments; j++){ 
                comment = "";
                if (j == 0) {
                    comment = Comments.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\r\n", "");
                }
                String[] commenttexts = {fechavenciments.get(j),importevenciments.get(j)+" "+DocCur,comment};
                for (String commenttext : commenttexts) {
                    PdfPCell comaux1 = new PdfPCell(new Paragraph(commenttext,f));
                    comaux1.setBorder(Rectangle.NO_BORDER);
                    if (commenttext.equals(commenttexts[commenttexts.length - 1])) {
                        comaux1.setIndent(20);
                        comaux1.setColspan(6);
                    }
                    comentarios.addCell(comaux1);
               }
            }
        } else if (!Comments.equals("")) { // only comments
            String commenttext = Comments.replaceAll("\n", "").replaceAll("\r", "").replaceAll("\r\n", "");
            if (mailtype.equals(ConfigStr.FACTURASUSA) || mailtype.equals(ConfigStr.PEDIDOSUSA))
                commenttext = commenttext.replaceAll("EE.UU.", "U.S.A.");
            
            PdfPCell comentlast = new PdfPCell(new Paragraph(getColumnNamePerLangauge(idioma, "COMMENT"), fb));
            comentlast.setBorder(Rectangle.NO_BORDER);
            comentlast.setColspan(8);
            comentarios.addCell(comentlast);
            PdfPCell comaux2;
            comaux2 = new PdfPCell(new Paragraph(commenttext, f));
            comaux2.setBorder(Rectangle.NO_BORDER);
            comaux2.setIndent(20);
            comaux2.setColspan(8);
            comentarios.addCell(comaux2);
        }
        this.add(comentarios);
    }

    private void generateDocTotal() throws DocumentException {
        //Tabla que contiene el resumen
        PdfPTable totales = new PdfPTable(9);
        totales.setWidthPercentage(100);
        totales.setSpacingBefore(5);
        String[] columnWidthsstr = Config.param(mailtype, ConfigStr.DOC_COLUMNS_WIDTHS2).split(",");
        float[] columnWidths2 = new float[columnWidthsstr.length];
        int icolumn = 0;
        for (String columnwidth : columnWidthsstr) {
            columnWidths2[icolumn++] = Float.parseFloat(columnwidth);
        }
        totales.setWidths(columnWidths2);
        String[] columnNames2 = Config.param(mailtype, ConfigStr.DOC_COLUMNS_TITLES2).split(",");
        PdfPCell totcol;
        for (String columnname : columnNames2) {
            totcol = new PdfPCell(new Paragraph(getColumnNamePerLangauge(idioma, columnname), fb));
            totcol.setBorder(Rectangle.BOX);
            totcol.setBorderColor(BaseColor.LIGHT_GRAY);
            totcol.setBorderColorLeft(BaseColor.WHITE);
            totcol.setBorderColorRight(BaseColor.WHITE);
            if (columnname.equals("INTRA"))
                totcol.setHorizontalAlignment(Element.ALIGN_LEFT);
            totcol.setBorderWidthLeft(0);
            totcol.setBorderWidthRight(0);
            if (!columnname.equals("AMOUNT") && !columnname.equals("DISCOUNT"))
                totcol.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totales.addCell(totcol);
        }
        
        String[] columnvalues2 = {roundNum(ptotal,mailtype),DiscPrcnt + "     " + DiscSum,TotalExpns,roundNum(Doctotal - VatSum,mailtype),
                                  roundNum(VatSum - sumre,mailtype),roundNum(re,mailtype) + "%",roundNum(sumre,mailtype),
                                  roundNum(Doctotal,mailtype) + " " + DocCur,""};
        if (mailtype.equals(ConfigStr.FACTURASUSA)) {
            Float resultIntraUSA = 0.04f * (ptotal + 33)  + 33;
            Float resultPtotal = ptotal + resultIntraUSA;
            columnvalues2 = new String[] {roundNum(ptotal,mailtype),"",roundNum(resultIntraUSA,mailtype),roundNum(0.00f,mailtype),
                                          roundNum(resultPtotal,mailtype),roundNum(resultPtotal,mailtype) + " " + DocCur,"","",""};
        }
        LogSeyma.printdebug("price total: " + roundNum(ptotal,mailtype));
        PdfPCell totval;
        int icol = 0;
        for (String columnvalue : columnvalues2) {
            totval = new PdfPCell(new Paragraph(columnvalue, f));
            totval.setBorder(Rectangle.NO_BORDER);
            if (icol > 1)
                totval.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totales.addCell(totval);
            icol++;
        }
        this.add(totales);
    }

    private int genereateItemsList(int i) throws BadElementException, IOException, SQLException, DocumentException {
        String[] columnWidthsstr = Config.param(mailtype, ConfigStr.DOC_COLUMNS_WIDTHS).split(",");
        float[] columnWidths = new float[columnWidthsstr.length];
        int icolumn = 0;
        for (String columnwidth : columnWidthsstr) {
            columnWidths[icolumn++] = Float.parseFloat(columnwidth);
        }
        //Tabla que contiene la lista de artículos
        PdfPTable listadoArticulos = new PdfPTable(columnWidths.length);
        listadoArticulos.setWidthPercentage(100);
        listadoArticulos.setSpacingBefore(2);
        listadoArticulos.setWidths(columnWidths);

        //Utilizaremos refBlank para hacer el sangrado de los subarticulos
        String columntitle_lang;
        PdfPCell pdfCell;
        int icell;
        //ArrayList<String> columnTitles = new ArrayList<>();                
        String[] columntitles = Config.param(mailtype, ConfigStr.DOC_COLUMNS_TITLES).split(",");
        for (String columntitle : columntitles) {
            if (((columntitle.equals("TARIF") || columntitle.equals("DTO")) && !dtoarticles) 
                    || (mailtype.equals(ConfigStr.PEDIDOSUSA) && (columntitle.equals("PVP") || columntitle.equals("TAX")))) {
                columntitle = "?";
            }
            columntitle_lang = getColumnNamePerLangauge(idioma,columntitle);
            pdfCell = new PdfPCell(new Paragraph(columntitle_lang,fb));
            pdfCell.setBorder(Rectangle.BOX);
            pdfCell.setBorderColor(BaseColor.LIGHT_GRAY);
            pdfCell.setBorderColorLeft(BaseColor.WHITE);
            pdfCell.setBorderColorRight(BaseColor.WHITE);
            pdfCell.setBorderWidthLeft(0);
            pdfCell.setBorderWidthRight(0);

            if (!(columntitle_lang.equals(" ") || columntitle.equals("REF") || columntitle.equals("DESC"))) { 
                pdfCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            }
            listadoArticulos.addCell(pdfCell);
        }
        
        PdfPCell itemimage;
        Font fdata = fcg;
        String pvpstr = "";
        String tarifstr = " ";
        String discstr = " ";
        String pricestr = "";
        String vatstr = "";
        String totalstr = "";
        int itemnumperpage = numeroArticulosPorPagina();
        for (int j = 0; j < itemnumperpage; j++, i++) {
            if (i < nitems) {
                if(mailtype.equals(ConfigStr.PEDIDOS) || mailtype.equals(ConfigStr.PEDIDOSUSA)) {
                    if (itemimg.get(i) != null){
                        itemimage = new PdfPCell(Image.getInstance(itemimg.get(i).getBytes(1, (int) itemimg.get(i).length())), true);
                    } else {
                        itemimage = new PdfPCell(new Paragraph(" "));
                    }
                    if (itemtreetype.get(i).equals("I")) {
                        itemimage.setHorizontalAlignment(Element.ALIGN_MIDDLE);
                    } else {
                        itemimage.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    }
                    itemimage.setBorder(Rectangle.NO_BORDER);
                    itemimage.setFixedHeight(40f);
                    itemimage.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    listadoArticulos.addCell(itemimage);
                }
                if (!idioma.equals("NL") && !idioma.equals("IT") && !CardCode.substring(0, 2).equals("CW") 
                        && !mailtype.equals(ConfigStr.PEDIDOSUSA)) {
                    pvpstr = roundNum(itempvpr.get(i),mailtype);
                }
                if (dtoarticles) {
                    tarifstr = roundNum(itemtarifa.get(i),mailtype);
                    discstr = roundNum(itemdiscount.get(i),mailtype);
                } 
                if (!itemtreetype.get(i).equals("I")) {
                    fdata = f;
                    vatstr = itemiva.get(i);
                    pricestr = itemprice.get(i);
                    totalstr = itemtotal.get(i);
                }
                String[] itemdata_l = {itemcodes.get(i),itemDesc.get(i)};
                addPropertiesCells(itemdata_l, listadoArticulos, fdata, Element.ALIGN_LEFT, Element.ALIGN_MIDDLE);
                String[] itemdata_r = {itemquant.get(i),pvpstr,tarifstr,discstr,vatstr,pricestr,totalstr};
                addPropertiesCells(itemdata_r, listadoArticulos, fdata, Element.ALIGN_RIGHT, Element.ALIGN_MIDDLE);
            } else {
                if (npagina == totalnpagina && !idioma.equals("ES") && 
                        (mailtype.equals(ConfigStr.ABONOS)  || mailtype.equals(ConfigStr.FACTURAS))) {
                    break;
                }
                String[] emptystrs = new String[columnWidths.length];
                Arrays.fill(emptystrs, " ");
                addPropertiesCells(emptystrs, listadoArticulos, f);
            }
        }
        
        if ((npagina == totalnpagina && !intrastatTienePaginaPropia()) || (npagina == (totalnpagina - 1) && intrastatTienePaginaPropia())) {
            //Cantidad total de artículos
            String[] totalstrs = new String[columnWidths.length];
            Arrays.fill(totalstrs, "");
            totalstrs[columnWidths.length - 8] = getColumnNamePerLangauge(idioma, "QTY") + " " + getColumnNamePerLangauge(idioma, "TOTAL") + ":";
            totalstrs[columnWidths.length - 7] = roundNum(qtotal,"0",mailtype);
            addPropertiesCells(totalstrs, listadoArticulos, fb,Element.ALIGN_RIGHT);
            LogSeyma.printdebug("Qty total: " + totalstrs[columnWidths.length - 7]);
        }
        this.add(listadoArticulos);
        return i;
    }

    private void generateRepreData() throws DocumentException {
        String pauxstr;
        //Párrafo que contiene el representante al que pertenece la fatura.
        if (mailtype.equals(ConfigStr.FACTURAS) && CardCode.substring(0, 2).equals("CW")) {
           pauxstr = "\n";
        } else {
           pauxstr = getColumnNamePerLangauge(idioma,"REPR") + SlpName;
        }
        Paragraph representante = new Paragraph(pauxstr, f);
        representante.setAlignment(Element.ALIGN_RIGHT);
        //representante.setAlignment(2); Hay que poner izquierda
        this.add(representante);
    }

    private void generatePayData() throws DocumentException {
        //tabla que contiene los datos datos de pago del documento
        if (npagina == 1) {
            PdfPTable tablaPago = new PdfPTable(2);
            tablaPago.setWidthPercentage(100);
            PdfPCell datosPago1 = new PdfPCell(new Paragraph(getColumnNamePerLangauge(idioma,"PAYM"), f));
            datosPago1.setBorder(Rectangle.NO_BORDER);
            datosPago1.setIndent(10);
            PdfPCell datosPago2 = new PdfPCell(new Paragraph(getColumnNamePerLangauge(idioma,"BANK"), f));
            datosPago2.setBorder(Rectangle.NO_BORDER);
            datosPago2.setIndent(20);
            //tabla auxiliar para separar las tablas.
            PdfPTable tpaux1 = new PdfPTable(1);
            PdfPCell paux1 = new PdfPCell(new Paragraph(DatosPago, f));
            paux1.setIndent(10);
            paux1.setLeading(1f, 1.2f);
            paux1.setBorderColor(BaseColor.LIGHT_GRAY);
            tpaux1.addCell(paux1);
            PdfPCell datosPago3 = new PdfPCell(tpaux1);
            datosPago3.setPaddingRight(10);
            datosPago3.setBorder(Rectangle.NO_BORDER);
            datosPago3.setIndent(10);
            //tabla auxiliar para separar las tablas.
            PdfPTable tpaux2 = new PdfPTable(1);
            // default value for bank details
            String pauxstr;
            if (mailtype.equals(ConfigStr.FACTURASUSA)  || mailtype.equals(ConfigStr.PEDIDOSUSA) 
                    || (mailtype.equals(ConfigStr.FACTURAS) && CardCode.substring(0, 2).equals("CW"))) {
                    pauxstr = "\n\n\n";
            } else if (!mailtype.equals(ConfigStr.PEDIDOS) && BankName != null && BankCode != null && Street != null && City != null) {
                    pauxstr = BankName + "\n" + Street + "\n" + City + "\n" + BankCode;
            } else if (!mailtype.equals(ConfigStr.PEDIDOS) && BankName != null && BankCode != null) {
                    pauxstr = BankName + "\n" + BankCode + "\n";
            } else if (mailtype.equals(ConfigStr.PEDIDOS) && idioma.equals("ES") && DatosPago.contains("GIRO") && BankName != null && BankCode != null) {
                    pauxstr = BankName + "\n" + BankCode + "\n";
            } else {
                    pauxstr = "\n\n\n";
            }
            
            PdfPCell paux2 = new PdfPCell(new Paragraph(pauxstr,f));
            paux2.setIndent(10);
            paux2.setLeading(1f, 1.2f);
            paux2.setBorderColor(BaseColor.LIGHT_GRAY);
            tpaux2.addCell(paux2);
            PdfPCell datosPago4 = new PdfPCell(tpaux2);
            datosPago4.setPaddingLeft(10);
            datosPago4.setBorder(Rectangle.NO_BORDER);
            datosPago4.setIndent(10);
            tablaPago.addCell(datosPago1);
            tablaPago.addCell(datosPago2);
            tablaPago.addCell(datosPago3);
            tablaPago.addCell(datosPago4);
            this.add(tablaPago);
        }
    }

    private void generateAddressData() throws DocumentException {
        /////////////////////////////////////////////////////////////////////////////
        //Tabla que contiene los datos de entrega y del doctumento
        PdfPTable tablaDirecciones = new PdfPTable(2);
        tablaDirecciones.setWidthPercentage(100);
        tablaDirecciones.setSpacingAfter(10);
        PdfPCell datosDirecciones1 = new PdfPCell(new Paragraph(getColumnNamePerLangauge(idioma,"ADDR1"), f));
        datosDirecciones1.setBorder(Rectangle.NO_BORDER);
        datosDirecciones1.setIndent(10);
        PdfPCell datosDirecciones2 = new PdfPCell(new Paragraph(getColumnNamePerLangauge(idioma,"ADDR2"), f));
        datosDirecciones2.setBorder(Rectangle.NO_BORDER);
        datosDirecciones2.setIndent(20);
        //tabla auxiliar para separar las tablas.
        PdfPTable tdaux1 = new PdfPTable(1);
        PdfPCell daux1 = new PdfPCell(new Paragraph(DirFiscal.replace("null",""), f));
        daux1.setIndent(10);
        daux1.setLeading(1f, 1.2f);
        daux1.setBorderColor(BaseColor.LIGHT_GRAY);
        tdaux1.addCell(daux1);
        PdfPCell datosDirecciones3 = new PdfPCell(tdaux1);
        datosDirecciones3.setIndent(10);
        datosDirecciones3.setPaddingRight(10);
        datosDirecciones3.setBorder(Rectangle.NO_BORDER);
        //tabla auxiliar para separar las tablas.
        PdfPTable tdaux2 = new PdfPTable(1);
        
        PdfPCell daux2 = new PdfPCell(new Paragraph(DirFactura.replace("null",""), f));
        daux2.setIndent(10);
        daux2.setLeading(1f, 1.2f);
        daux2.setBorderColor(BaseColor.LIGHT_GRAY);
        tdaux2.addCell(daux2);
        PdfPCell datosDirecciones4 = new PdfPCell(tdaux2);
        datosDirecciones4.setIndent(10);
        datosDirecciones4.setPaddingLeft(10);
        datosDirecciones4.setBorder(Rectangle.NO_BORDER);
        tablaDirecciones.addCell(datosDirecciones1);
        tablaDirecciones.addCell(datosDirecciones2);
        tablaDirecciones.addCell(datosDirecciones3);
        tablaDirecciones.addCell(datosDirecciones4);
        this.add(tablaDirecciones);
    }

    private void generateCompanyData() throws DocumentException {
        /////////////////////////////////////////////////////////////////////////////
        //Tabla que contiene los datos de contacto de la empresa
        String[] contactdetails1 = Config.param(ConfigStr.CONTACT_DETAILS1).split(";");
        String[] contactdetails2 = Config.param(ConfigStr.CONTACT_DETAILS2).split(";");
        if (mailtype.equals(ConfigStr.FACTURASUSA) || mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            contactdetails1 = Config.param(ConfigStr.CONTACT_DETAILS1_USA).split(";");
            contactdetails2 = Config.param(ConfigStr.CONTACT_DETAILS2_USA).split(";");            
        }
        
        PdfPTable tablaEncabezado2 = new PdfPTable(4);
        tablaEncabezado2.setWidthPercentage(100);
        tablaEncabezado2.setSpacingAfter(10);
        addPropertiesCells(contactdetails1, tablaEncabezado2, f);
        addPropertyCell(getColumnNamePerLangauge(idioma,"PAGE") + npagina + "/" + totalnpagina, tablaEncabezado2, f,Element.ALIGN_RIGHT);
        addPropertiesCells(contactdetails2, tablaEncabezado2, f);
        addPropertyCell(getColumnNamePerLangauge(idioma,"CCODE") + CardCode, tablaEncabezado2, f,Element.ALIGN_RIGHT);
        this.add(tablaEncabezado2);
    }

    private void generateDocHeader() throws DocumentException {
        PdfPCell datosVacio = new PdfPCell(new Paragraph("", f));
        datosVacio.setBorder(Rectangle.NO_BORDER);
        PdfPTable tablaEncabezado1 = new PdfPTable(2);
        PdfPTable tablaEncabezado2 = new PdfPTable(4);
        tablaEncabezado1.setWidthPercentage(100);
        tablaEncabezado2.setWidthPercentage(100);
        PdfPTable tablaEncabezado = tablaEncabezado1;
        String companyname = CompnyName; 
        String cif = CompnyNIF;
        String str3 = ""; 
        String str4 = "Nº EORI/VAT: " + CompnyNIF;
        String docnum = DocNum;
        if (mailtype.equals(ConfigStr.FACTURASUSA)  || mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            companyname = "ABBACINO USA LLC";
            cif = "47-3013073";
            str3 = "4801 BARBARA'S LANE";
            str4 = "STEVENS POINT, WI 54481";
            if (mailtype.equals(ConfigStr.FACTURASUSA))
                docnum = DocNumUSA;
        }
        addPropertyCell(companyname, tablaEncabezado, h1);
        addPropertyCell(getColumnNamePerLangauge(idioma,"TITLE"), tablaEncabezado, h1, Element.ALIGN_RIGHT);
        if (mailtype.equals(ConfigStr.FACTURASUSA) || mailtype.equals(ConfigStr.PEDIDOSUSA)) {    
            this.add(tablaEncabezado);
            tablaEncabezado = tablaEncabezado2;
        }
        
        addPropertyCell("CIF: " + cif, tablaEncabezado, h3b);
        if (mailtype.equals(ConfigStr.FACTURASUSA) || mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            addPropertyCell(str3, tablaEncabezado, h3b);
            tablaEncabezado.addCell(datosVacio);
        }
        addPropertyCell(getColumnNamePerLangauge(idioma,"NUM") + docnum, tablaEncabezado, f, Element.ALIGN_RIGHT);
        if (mailtype.equals(ConfigStr.FACTURASUSA) || mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            tablaEncabezado.addCell(datosVacio);
        }
        addPropertyCell(str4, tablaEncabezado, h3b);
        if (mailtype.equals(ConfigStr.FACTURASUSA) || mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            tablaEncabezado.addCell(datosVacio);
        }
        addPropertyCell(getColumnNamePerLangauge(idioma,"DATE") + DocDate, tablaEncabezado, f, Element.ALIGN_RIGHT);
        this.add(tablaEncabezado);
    }
    
    private void iniLangMap() {
        String[] columnnames = Config.param(ConfigStr.LANG_MAP_NAMES).split(",");
        String[] columnvaluesES = Config.param(ConfigStr.LANG_MAP_VALUES_ES).split(",");
        String[] columnvaluesEN = Config.param(ConfigStr.LANG_MAP_VALUES_EN).split(",");
        namesES = new HashMap<>();
        namesEN = new HashMap<>();
        namesES.put("TITLE",Config.param(mailtype, ConfigStr.DOC_TITLE_ES));
        namesEN.put("TITLE",Config.param(mailtype, ConfigStr.DOC_TITLE_EN));
        int i = 0;
        for (String columnname : columnnames) {
            namesES.put(columnname,columnvaluesES[i]);
            namesEN.put(columnname,columnvaluesEN[i]);
            i++;            
        }
        String totalstrES = namesES.get("TITLE");
        String totalstrEN = namesEN.get("TITLE");
        if (mailtype.equals(ConfigStr.PEDIDOS) || mailtype.equals(ConfigStr.PEDIDOSUSA)) {
            totalstrES = totalstrES.split(" ")[1];
            totalstrEN = totalstrEN.split(" ")[0];
        } 
        namesES.put("DOC_TTL","TOTAL " + totalstrES);
        namesEN.put("DOC_TTL",totalstrEN + " TOTAL");
    }
    
    private String getColumnNamePerLangauge(String lang, String iname) {
        HashMap<String, String> namesLang;
        if (iname.equals("PVP") && (lang.equals("NL") || lang.equals("IT") || CardCode.substring(0, 2).equals("CW"))) {
            return "";
        } else if (iname.equals("?")) {
            return " ";
        } else if (iname.equals("INTRA")) {
            return "INTRA-US SHIPPING AND HANDLING";
        } else if (lang.equals("ES") || lang.equals("AD")) {
            namesLang = namesES;
        } else {
            namesLang = namesEN;
        }        
        return namesLang.get(iname);
    }
        
}
