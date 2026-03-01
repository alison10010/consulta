package com.consulta.util;

import java.time.Year;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class VerificacaoDocsEmail {

    @Value("${app.urlSite}")
    private String urlSite;

    private final JavaMailSender mailSender;

    public VerificacaoDocsEmail(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviaEmailResultadoAnalise(String email, String nomeProfissional, String especialidade, String status, String motivo) {
        
    	email = "alisonlimabandeira@gmail.com";
    	
        boolean aprovado = "APROVADO".equalsIgnoreCase(status);
        String assunto = aprovado ? "✅ Especialidade Aprovada!" : "⚠️ Atualização sobre sua Especialidade";
        
        // Cores e ícones dinâmicos
        String corPrimaria = aprovado ? "#10b981" : "#ef4444"; // Verde vs Vermelho
        String bgBanner = aprovado ? "rgba(16,185,129,.10)" : "rgba(239,68,68,.10)";
        String statusTexto = aprovado ? "APROVADA" : "NÃO APROVADA";
        String icone = aprovado ? "✅" : "❌";

        String html = """
            <!doctype html>
            <html lang="pt-BR">
            <head>
              <meta charset="utf-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1"/>
            </head>
            <body style="margin:0;padding:0;background:#f8fafc;font-family:sans-serif;color:#0f172a;">
              <table role="presentation" width="100%%" style="background:#f8fafc; padding:28px 12px;">
                <tr>
                  <td align="center">
                    <table role="presentation" width="600" style="max-width:600px; width:100%%; background:#ffffff; border-radius:20px; border:1px solid rgba(15,23,42,.10); overflow:hidden;">
                      
                      <tr>
                        <td style="padding:20px; background:%s; border-bottom:1px solid rgba(15,23,42,.08);">
                          <div style="font-size:18px; font-weight:900;">%s Análise de Especialidade</div>
                        </td>
                      </tr>

                      <tr>
                        <td style="padding:24px;">
                          <div style="font-size:16px; margin-bottom:12px;">Olá, <strong>%s</strong>.</div>
                          <div style="font-size:14px; color:#475569; line-height:1.6;">
                            Informamos que a análise dos seus documentos para a especialidade 
                            <strong style="color:#0f172a;">%s</strong> foi concluída.
                          </div>

                          <div style="margin:20px 0; padding:16px; border-radius:12px; background:#f1f5f9; text-align:center;">
                            <span style="font-size:12px; text-transform:uppercase; letter-spacing:1px; color:#64748b; display:block; margin-bottom:4px;">Resultado:</span>
                            <span style="font-size:20px; font-weight:900; color:%s;">%s</span>
                          </div>

                          %s

                          <div style="margin-top:24px;">
                            <a href="%s" style="display:inline-block; padding:12px 24px; background:%s; color:#ffffff; text-decoration:none; border-radius:12px; font-weight:bold; font-size:14px;">
                              %s
                            </a>
                          </div>
                        </td>
                      </tr>

                      <tr>
                        <td style="padding:16px 24px; background:#f8fafc; font-size:12px; color:#94a3b8; border-top:1px solid rgba(15,23,42,.05);">
                          © %d Consulta Médica Online. Este é um e-mail automático.
                        </td>
                      </tr>
                    </table>
                  </td>
                </tr>
              </table>
            </body>
            </html>
            """.formatted(
                bgBanner, 
                icone, 
                nomeProfissional, 
                especialidade, 
                corPrimaria, 
                statusTexto,
                aprovado ? "" : "<div style='padding:12px; border-left:4px solid #ef4444; background:#fff1f2; font-size:13px;'><strong>Motivo:</strong> " + motivo + "</div>",
                urlSite,
                corPrimaria,
                aprovado ? "Acessar Painel" : "Corrigir Documentos",
                Year.now().getValue()
            );

        enviar(email, assunto, html);
    }

    private void enviar(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(to);
            helper.setFrom("espcecialistasuporte@gmail.com");
            helper.setSubject(subject);
            helper.setText(content, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Erro ao enviar e-mail", e);
        }
    }
}