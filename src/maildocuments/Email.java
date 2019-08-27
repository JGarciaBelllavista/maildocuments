/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package maildocuments;

import abbacinoutils.Config;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author lorenzo
 */
public class Email {

    private String mailtype;
    private String to;
    private String mailrepre;
    private String mailmanager;
    private String nombredestinatario;
    private String asunto;
    private String cuerpo;
    private String urldocumento;
    private String nombredocumento;
    private String documento;
    private String numUSA;
    private boolean enviado;
    private String cardcode;

    /**
     * @param to
     * @param mailrepre Mail del representante
     * @param mailmanager
     * @param nombredestinatario
     * @param asunto
     * @param cuerpo
     * @param urldocumento
     * @param nombredocumento
     * @param documento
     * @param companyText
     * @param marcaCorporativa
     * @param cardcode
     * @param i_mailtype
     */
    public Email(String to, String mailrepre, String mailmanager, String nombredestinatario, String asunto, String cuerpo,
            String urldocumento, String nombredocumento, String documento, String companyText, String marcaCorporativa, String cardcode, String i_mailtype) {
        this.mailtype = i_mailtype;
        this.to = to;
        this.mailrepre = mailrepre;
        this.mailmanager = mailmanager;
        this.nombredestinatario = nombredestinatario;
        this.asunto = asunto;
        this.urldocumento = urldocumento;
        this.nombredocumento = nombredocumento;
        this.documento = documento;
        this.cardcode = cardcode;
        this.enviado = false;
        this.cuerpo = getEmailHtml(Config.param(mailtype, ConfigStr.DOC_HTML))
                                   .replace(Config.param(ConfigStr.SIGN_RPL), Config.param(ConfigStr.SIGN_IMG))
                                   .replace(Config.param(ConfigStr.BODY_RPL), cuerpo)
                                   .replace(Config.param(ConfigStr.COMPANY_RPL), companyText)
                                   .replace(Config.param(ConfigStr.MARCA_RPL), marcaCorporativa);
    }

    public String getMailtype() {
        return mailtype;
    }

    public void setMailtype(String mailtype) {
        this.mailtype = mailtype;
    }

    public void setCardcode(String cardcode) {
        this.cardcode = cardcode;
    }

    /**
     * Función para enviar un mail del estado del envio de confirmaciones de
     * documento.
     *
     * @param pedidos
     * @param total
     * @param incorrectos
     * @param i_mailtype
     * @param company
     */
    public Email(ArrayList<String> pedidos, int total, int incorrectos, String i_mailtype, String company) {
        this.mailtype = i_mailtype;
        this.asunto = Config.param(ConfigStr.EMAIL_RESULT_SUBJ) + company + ", " + i_mailtype;
        this.cuerpo = Config.param(ConfigStr.EMAIL_RESULT_BODY).replace(Config.param(ConfigStr.TOTAL_RPL),String.valueOf(total))
                                                            .replace(Config.param(ConfigStr.MAILTYPE_RPL),i_mailtype)
                                                            .replace(Config.param(ConfigStr.NOT_SENT_RPL),String.valueOf(incorrectos));
        if (incorrectos > 0) {
            for (int i = 0; i < incorrectos; i++) {
                this.cuerpo += pedidos.get(i) + ";\n";
            }
            this.asunto = "ERROR: " + this.asunto;
        } else {
            this.cuerpo = this.cuerpo.split(",")[0] + ". Todos los documentos se han enviado con exito";
        }
    }

    private String getEmailHtml(String filename) {
        BufferedReader reader;
        String line;
        String bodyText = "";
        try {
            reader = new BufferedReader(new FileReader(filename));
            try {
                while ((line = reader.readLine()) != null) {
                    bodyText += line;
                }
            } catch (IOException ex) {
                Logger.getLogger(Conexion.class.getName()).log(Level.SEVERE, null, ex);
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Conexion.class.getName()).log(Level.SEVERE, null, ex);
        }
        return bodyText;
    }

    /**
     * @return the asunto
     */
    public String getAsunto() {
        return asunto;
    }

    /**
     * @return the cuerpo
     */
    public String getCuerpo() {
        return cuerpo;
    }

    /**
     * @return the to
     */
    public String getTo() {
        if (to == null) {
            return "-";
        } else {
            return to;
        }
    }

    /**
     * @return the nombredestinatario
     */
    public String getNombredestinatario() {
        return nombredestinatario;
    }

    /**
     * @return the urldocumento
     */
    public String getUrldocumento() {
        return urldocumento;
    }

    /**
     * @return the nombredocumento
     */
    public String getNombredocumento() {
        return nombredocumento;
    }

    public String getNumUSA() {
        return numUSA;
    }

    public void setNumUSA(String numUSA) {
        this.numUSA = numUSA;
    }

    /**
     * @return the mailrepre
     */
    public String getMailrepre() {
        if (mailrepre == null) {
            return "-";
        } else {
            return mailrepre;
        }
    }

    /**
     * @return the mailmanager
     */
    public String getMailmanager() {
        if (mailmanager == null) {
            return "-";
        } else {
            return mailmanager;
        }
    }

    /**
     * @return the documento
     */
    public String getDocumento() {
        return documento;
    }

    /**
     * @return the cardcode
     */
    public String getCardCode() {
        return cardcode;
    }

    /**
     * @return the enviado
     */
    public boolean isEnviado() {
        return enviado;
    }

    /**
     * @param enviado the enviado to set
     */
    public void setEnviado(boolean enviado) {
        this.enviado = enviado;
    }

}
