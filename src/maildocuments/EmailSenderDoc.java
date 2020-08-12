/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package maildocuments;

import abbacinoutils.Config;
import abbacinoutils.EmailSender;
import abbacinoutils.LogSeyma;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimeUtility;

/**
 *
 * @author lorenzo
 */
public class EmailSenderDoc extends EmailSender {
    
    boolean debug = Config.param(ConfigStr.DEBUG).equals("true");

    public void send(Email correu) {
        //Iniciamos sesion antes de enviar el mensaje
        String emails = "";
        Session session = Session.getDefaultInstance(getProps(), new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getUser_noreply(), getKey_noreply());
            }
        });

        try {
            //Creamos el mensaje y lo inicializamos
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(getUser_noreply(), ""));

            // Add client email in TO
            
            if (correu.getCardCode().startsWith("CN")) {
                String[] cn_mailinglist = Config.param(ConfigStr.CN_EMAILS).split(";");
                if (!debug)
                    msg.addRecipient(Message.RecipientType.TO, new InternetAddress(cn_mailinglist[0]));
                LogSeyma.printdebug("addRecipient TO: " + cn_mailinglist[0]);
                if (!debug)
                    msg.addRecipient(Message.RecipientType.CC, new InternetAddress(cn_mailinglist[1]));
                LogSeyma.printdebug("addRecipient CC: " + cn_mailinglist[1]);

            } else if (!correu.getTo().equals("-")) {
                msg = addRecipientsList(correu.getTo(), Message.RecipientType.TO, msg);
            }
            
            // Add representante email in CC
            if (correu.getMailtype().equals(ConfigStr.VENCIMIENTOS)) {
                List<String> notemaillist0 = Arrays.asList(Config.param(correu.getMailtype(), ConfigStr.NOT_EMAIL_LIST + "0").split(";"));
                if (!notemaillist0.contains(correu.getMailrepre())) {
                    msg = addRecipientsList(correu.getMailrepre(), Message.RecipientType.BCC, msg);
                    LogSeyma.printdebug("addRecipient BCC: " + correu.getMailrepre());
                }
            } else if (!correu.getMailrepre().equals("-")) {
                RecipientType recipientype = Message.RecipientType.CC;
                if (correu.getMailtype().equals("PEDIDOS"))
                    recipientype = Message.RecipientType.TO;
                msg = addRecipientsList(correu.getMailrepre(), recipientype, msg);
                LogSeyma.printdebug("addRecipient " + recipientype + ": " + correu.getMailrepre());
            }
                        
            // Add manager email in CC
            List<String> notemaillist = Arrays.asList(Config.param(correu.getMailtype(), ConfigStr.NOT_EMAIL_LIST).split(";"));
            if (!notemaillist.contains(correu.getMailrepre()) && !correu.getMailmanager().equals("-")) {
                msg = addRecipientsList(correu.getMailmanager(), Message.RecipientType.CC, msg);
                LogSeyma.printdebug("addRecipient CC: " + correu.getMailmanager());
            }

            // Add compay email in BCC
            String bccemaillist = Config.param(correu.getMailtype(),ConfigStr.BCC_EMAIL_LIST);
            if (correu.getCardCode().startsWith("CN")) {
                for (String email2remove : Config.param(ConfigStr.CN_EMAILS).split(";")) {
                    bccemaillist = bccemaillist.replace(email2remove + ";","");
                    LogSeyma.printdebug("Removing BCC Email " + email2remove);
                    LogSeyma.printdebug("bccemaillist " + bccemaillist);
                }
            }
                
            RecipientType recipientype = Message.RecipientType.BCC;
            if (correu.getMailtype().equals(ConfigStr.PEDIDOS) || correu.getMailtype().equals(ConfigStr.PEDIDOSUSA))
                recipientype = Message.RecipientType.CC;
            msg = addRecipientsList(bccemaillist, recipientype, msg);
            //send to logs only
            if (debug)
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(Config.param(Config.TO_EMAIL)));
            
            //Añadimos el asunto al mensaje
            String subject = correu.getAsunto();
            if (debug)
               subject = "TEST: " + subject;
            msg.setSubject(MimeUtility.encodeText(subject, "utf-8", "B"));
            //Creamos la parte del mensaje correspondiente al cuerpo en texto plano
            BodyPart texto = new MimeBodyPart();
            texto.setContent(correu.getCuerpo(), "text/html; charset=UTF-8");
            //Creamos el formato para juntar ambas partes
            MimeMultipart multiParte = new MimeMultipart();
            //Metemos ambas partes en el formato creado anteriormente
            multiParte.addBodyPart(texto);
            if (!correu.getUrldocumento().equals("")) { // No need to attach if file no exists
                //Creamos la parte del mensaje correspondiente al archivo adjunto
                BodyPart adjunto = new MimeBodyPart();
                adjunto.setDataHandler(new DataHandler(new FileDataSource(correu.getUrldocumento())));
                adjunto.setFileName(correu.getNombredocumento());
                multiParte.addBodyPart(adjunto);
                //Añadimos todo al contenido del correo
            }
            msg.setContent(multiParte);
            //Enviamos el mensaje.            
            Address[] allAddresses = msg.getAllRecipients();
            for (Address address : allAddresses) {
                emails += address.toString() + ", ";
            }
            Transport.send(msg);
            correu.setEnviado(true);
            LogSeyma.println("Email was sent to " + emails + " subject: " + correu.getAsunto());
        } catch (AddressException e) {
            LogSeyma.printexcp("Error in email address " + emails + " : " + e.getMessage());
        } catch (MessagingException e) {
            LogSeyma.printexcp("Error sending email " + emails + " : " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            LogSeyma.printexcp("Error unsupported encoding " + emails + " : " + e.getMessage());
            Logger.getLogger(EmailSenderDoc.class.getName()).log(Level.SEVERE, null, e);
        }
    }

    public void sendConfirmation(Email correu) {
        //Iniciamos sesion antes de enviar el mensaje
        Session session = Session.getDefaultInstance(getProps(), new javax.mail.Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(getUser_noreply(), getKey_noreply());
            }
        });

        try {
            //Creamos el mensaje y lo inicializamos
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress(getUser_noreply(), ""));
            if (debug)
                msg.addRecipient(RecipientType.TO, new InternetAddress(Config.param(Config.TO_EMAIL)));

            msg = addRecipientsList(Config.param(ConfigStr.TO_EMAILCONFITM_LIST), Message.RecipientType.TO, msg);
            // For testing//
            String subject = correu.getAsunto();
            if (debug)
                subject = "TEST: " + subject;
            msg.setSubject(subject);
            msg.setText(correu.getCuerpo());
            Transport.send(msg);
            LogSeyma.println("Confirmation email was sent");
        } catch (AddressException e) {
            LogSeyma.printexcp("Error in email address: " + e.getMessage());
        } catch (MessagingException e) {
            LogSeyma.printexcp("Error sending email: " + e.getMessage());
        } catch (UnsupportedEncodingException e) {
            LogSeyma.printexcp("Error unsupported encoding: " + e.getMessage());
            Logger.getLogger(EmailSenderDoc.class.getName()).log(Level.SEVERE, null, e);
        }
    }
    
    private Message addRecipientsList(String recipientsliststr, RecipientType type, Message msg) throws AddressException, MessagingException {
        String[] recipientslist = recipientsliststr.split(";");
        for (String emailrecipient : recipientslist) {
            if (!debug)
                msg.addRecipient(type, new InternetAddress(emailrecipient));
            LogSeyma.printdebug("addRecipient " + type + ": " + emailrecipient);
        }
        return msg;
    }
}
