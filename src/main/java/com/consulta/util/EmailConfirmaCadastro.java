package com.consulta.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailConfirmaCadastro {
	
	@Value("${app.urlSite}")
    private String urlSite;

    private final JavaMailSender mailSender;

    public EmailConfirmaCadastro(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviaMensagem(String email, String hash) {
    	
       email = "alisonlimabandeira@gmail.com";
    	
    	
	   String assunto = "Confirme seu cadastro";

	   String confirmUrl = urlSite + "/consulta/api/usuario/confirmar-cadastro?hash=" + 
	   java.net.URLEncoder.encode(hash, java.nio.charset.StandardCharsets.UTF_8);

    	    String html = """
    	<!doctype html>
    	<html lang="pt-BR">
    	<head>
    	  <meta charset="utf-8"/>
    	  <meta name="viewport" content="width=device-width, initial-scale=1"/>
    	  <title>Confirmação</title>
    	</head>
    	<body style="
    	  margin:0;
    	  padding:0;
    	  background:#f8fafc;
    	  font-family: Inter, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif;
    	  color:#0f172a;
    	">
    	  <!-- Wrapper -->
    	  <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%%"
    	         style="background:#f8fafc; padding:28px 12px;">
    	    <tr>
    	      <td align="center">

    	        <!-- Container -->
    	        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="600"
    	               style="max-width:600px; width:100%%;">
    	          <tr>
    	            <td style="padding:0 6px 14px 6px;">
    	              <!-- Top brand row -->
    	              <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" border="0">
    	                <tr>
    	                  <td align="left" style="font-weight:800; font-size:14px; letter-spacing:.3px;">
    	                    <span style="
    	                      display:inline-block;
    	                      padding:8px 12px;
    	                      border-radius:999px;
    	                      background: rgba(43,108,255,.10);
    	                      border: 1px solid rgba(15,23,42,.08);
    	                    ">
    	                      <span style="color:#2b6cff;">Consulta</span>
    	                      <span style="color:#7a5cff;">Médica Online</span>
    	                    </span>
    	                  </td>
    	                  <td align="right" style="font-size:12px; color:rgba(15,23,42,.55);">
    	                    Confirmação de cadastro
    	                  </td>
    	                </tr>
    	              </table>
    	            </td>
    	          </tr>

    	          <tr>
    	            <td style="
    	              border-radius:20px;
    	              background:#ffffff;
    	              border:1px solid rgba(15,23,42,.10);
    	              box-shadow: 0 14px 40px rgba(15,23,42,.10);
    	              overflow:hidden;
    	            ">

    	              <!-- Header strip -->
    	              <div style="
    	                padding:18px 20px;
    	                background: linear-gradient(90deg, rgba(43,108,255,.10), rgba(122,92,255,.10));
    	                border-bottom:1px solid rgba(15,23,42,.08);
    	              ">
    	                <div style="font-size:18px; font-weight:900; line-height:1.2;">
    	                  Falta só mais 1 passo ✅
    	                </div>
    	                <div style="margin-top:6px; font-size:13px; color:rgba(15,23,42,.65);">
    	                  Clique no botão abaixo para confirmar seu cadastro com segurança.
    	                </div>
    	              </div>

    	              <!-- Body -->
    	              <div style="padding:20px;">
    	                <div style="font-size:14px; color:rgba(15,23,42,.78); line-height:1.6;">
    	                  Se você fez esse cadastro, confirme agora. Caso não tenha sido você, ignore este e-mail.
    	                </div>

    	                <!-- Button -->
    	                <table role="presentation" cellpadding="0" cellspacing="0" border="0" style="margin:18px 0 10px 0;">
    	                  <tr>
    	                    <td align="center" bgcolor="#2b6cff" style="border-radius:14px;">
    	                      <a href="%s" target="_blank" style="
    	                        display:inline-block;
    	                        padding:12px 18px;
    	                        font-weight:900;
    	                        font-size:14px;
    	                        color:#ffffff;
    	                        text-decoration:none;
    	                        border-radius:14px;
    	                      ">
    	                        Confirmar cadastro
    	                      </a>
    	                    </td>
    	                  </tr>
    	                </table>

    	                <div style="font-size:12px; color:rgba(15,23,42,.55); line-height:1.6;">
    	                  Se o botão não abrir, copie e cole este link no navegador:
    	                  <div style="
    	                    margin-top:8px;
    	                    word-break:break-all;
    	                    padding:10px 12px;
    	                    border-radius:12px;
    	                    background:rgba(15,23,42,.04);
    	                    border:1px solid rgba(15,23,42,.08);
    	                    font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, 'Liberation Mono', 'Courier New', monospace;
    	                    font-size:11px;
    	                    color:rgba(15,23,42,.75);
    	                  ">%s</div>
    	                </div>
    	              </div>

    	              <!-- Footer -->
    	              <div style="
    	                padding:14px 20px;
    	                border-top:1px solid rgba(15,23,42,.08);
    	                background:#ffffff;
    	                font-size:12px;
    	                color:rgba(15,23,42,.55);
    	              ">
    	                Este link pode expirar por segurança.
    	              </div>

    	            </td>
    	          </tr>

    	          <tr>
    	            <td align="center" style="padding:14px 6px 0 6px; font-size:12px; color:rgba(15,23,42,.55);">
    	              © %d Consulta Médica Online • Mensagem automática, não responda.
    	            </td>
    	          </tr>
    	        </table>

    	      </td>
    	    </tr>
    	  </table>
    	</body>
    	</html>
    	""".formatted(confirmUrl, confirmUrl, java.time.Year.now().getValue());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo(email);
            helper.setFrom("espcecialistasuporte@gmail.com");
            helper.setSubject(assunto);
            helper.setText(html, true); // true = HTML

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Erro ao enviar e-mail de confirmação", e);
        }
    }
}
