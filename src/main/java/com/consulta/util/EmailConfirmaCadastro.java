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
        // Remova a linha abaixo em produção para usar o e-mail do parâmetro
         email = "alisonlimabandeira@gmail.com"; 
        
        String assunto = "Confirme seu cadastro - Consulta Médica Online";
        String confirmUrl = urlSite + "/consulta/api/usuario/confirmar-cadastro?hash=" + 
                java.net.URLEncoder.encode(hash, java.nio.charset.StandardCharsets.UTF_8);

        String html = """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            @import url('https://fonts.googleapis.com/css2?family=Segoe+UI:wght@400;600;700&display=swap');
          </style>
        </head>
        <body style="margin:0; padding:0; background-color:#f4f7fa; font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;">
          <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#f4f7fa; padding: 40px 10px;">
            <tr>
              <td align="center">
                <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width:550px; background-color:#ffffff; border-radius:12px; overflow:hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.05); border: 1px solid #e1e8f0;">
                  
                  <tr>
                    <td style="padding: 30px 40px; background-color: #ffffff; text-align: left; border-bottom: 1px solid #f0f4f8;">
                      <span style="font-size: 20px; font-weight: 700; color: #2b6cff;">Consulta <span style="color: #7a5cff;">Médica</span></span>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding: 40px;">
                      <h1 style="margin: 0 0 20px; font-size: 24px; color: #1a202c; font-weight: 700; line-height: 1.3;">
                        Olá!
                      </h1>
                      <p style="margin: 0 0 30px; font-size: 16px; color: #4a5568; line-height: 1.6;">
                        Recebemos sua solicitação de cadastro na <strong>Consulta Médica Online</strong>. Para garantir a segurança dos seus dados, precisamos que confirme seu e-mail clicando no botão abaixo:
                      </p>

                      <table role="presentation" border="0" cellspacing="0" cellpadding="0" style="margin: 30px 0;">
                        <tr>
                          <td align="center" style="border-radius: 8px;" bgcolor="#2b6cff">
                            <a href="%s" target="_blank" style="font-size: 16px; font-weight: 600; color: #ffffff; text-decoration: none; padding: 14px 30px; border-radius: 8px; display: inline-block;">
                              Confirmar meu E-mail
                            </a>
                          </td>
                        </tr>
                      </table>

                      <p style="margin: 30px 0 10px; font-size: 14px; color: #718096;">
                        Se o botão acima não funcionar, copie e cole o link abaixo no seu navegador:
                      </p>
                      <div style="background-color: #f8fafc; border: 1px solid #edf2f7; padding: 12px; border-radius: 6px; word-break: break-all;">
                        <a href="%s" style="font-size: 12px; color: #2b6cff; text-decoration: none; font-family: monospace;">%s</a>
                      </div>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding: 20px 40px; background-color: #f8fafc; border-top: 1px solid #f0f4f8; font-size: 13px; color: #a0aec0; text-align: center;">
                      Este é um e-mail automático, por favor não responda.
                    </td>
                  </tr>
                </table>

                <table role="presentation" width="100%%" border="0" cellspacing="0" cellpadding="0" style="max-width:550px; margin-top: 20px;">
                  <tr>
                    <td style="text-align: center; font-size: 12px; color: #a0aec0; line-height: 1.5;">
                      © %d Consulta Médica Online. Todos os direitos reservados.<br>                      
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(confirmUrl, confirmUrl, confirmUrl, java.time.Year.now().getValue());

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setTo(email);
            helper.setFrom("especialistasuporte@gmail.com"); // Corrigi o erro de digitação de 'espcecialista'
            helper.setSubject(assunto);
            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Erro ao enviar e-mail de confirmação", e);
        }
    }
}