package com.consulta.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.consulta.record.AdminKpisDTO;
import com.consulta.record.EspecialidadeQtdDTO;
import com.consulta.repository.RelatorioRepository;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RelatorioEmail {

    private final RelatorioRepository relatorioRepository;
    private final JavaMailSender mailSender;

    private static final String[] EMAILS_FIXOS = {
            "alisonlimabandeira@gmail.com"
    };

    private static final DateTimeFormatter DATA_BR =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final NumberFormat MOEDA_BR =
            NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));

    @Transactional(readOnly = true)
    public void enviarRelatorioDiario() {

        LocalDate hoje = LocalDate.now();

        LocalDate ini = hoje.withDayOfMonth(1);
        LocalDate fim = hoje;

        LocalDateTime iniDt = ini.atStartOfDay();
        LocalDateTime fimDt = fim.plusDays(1).atStartOfDay().minusNanos(1);

        AdminKpisDTO kpis = relatorioRepository.buscarKpis(ini, fim, iniDt, fimDt);
        List<EspecialidadeQtdDTO> ranking = relatorioRepository.rankingEspecialidades();

        String html = montarCorpoEmailHtml(ini, fim, kpis, ranking);

        try {
            MimeMessage message = mailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    true,
                    "UTF-8"
            );

            helper.setTo(EMAILS_FIXOS);
            helper.setFrom("especialistasuporte@gmail.com");
            helper.setSubject("Relatório diário do sistema - " + hoje.format(DATA_BR));
            helper.setText(html, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("Erro ao enviar relatório diário por e-mail", e);
        }
    }

    private String montarCorpoEmailHtml(
            LocalDate ini,
            LocalDate fim,
            AdminKpisDTO kpis,
            List<EspecialidadeQtdDTO> ranking
    ) {

        if (kpis == null) {
            return """
            <html>
            <body>
                <h2>Relatório diário do sistema</h2>
                <p>Não foi possível carregar os dados do relatório.</p>
            </body>
            </html>
            """;
        }

        BigDecimal totalPago = kpis.totalPagoPeriodo() == null
                ? BigDecimal.ZERO
                : kpis.totalPagoPeriodo();

        String rankingHtml = montarRankingHtml(ranking);

        return """
        <!DOCTYPE html>
        <html lang="pt-BR">
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>

        <body style="margin:0; padding:0; background:#f4f7fa; font-family:Segoe UI, Arial, sans-serif;">

          <table width="100%%" cellspacing="0" cellpadding="0" style="background:#f4f7fa; padding:35px 10px;">
            <tr>
              <td align="center">

                <table width="100%%" cellspacing="0" cellpadding="0" style="max-width:720px; background:#ffffff; border-radius:14px; overflow:hidden; border:1px solid #e5eaf0; box-shadow:0 4px 14px rgba(0,0,0,.06);">

                  <tr>
                    <td style="padding:28px 35px; background:linear-gradient(135deg,#2b6cff,#7a5cff); color:#ffffff;">
                      <h1 style="margin:0; font-size:25px;">Relatório diário</h1>
                      <p style="margin:8px 0 0; font-size:14px; opacity:.95;">
                        Consulta Médica Online
                      </p>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding:25px 35px; border-bottom:1px solid #edf2f7;">
                      <p style="margin:0; color:#4a5568; font-size:15px;">
                        Período analisado:
                        <strong style="color:#1a202c;">%s até %s</strong>
                      </p>
                    </td>
                  </tr>

                  <tr>
                    <td style="padding:25px 35px;">

                      <h2 style="font-size:18px; color:#1a202c; margin:0 0 15px;">Resumo de usuários</h2>

                      <table width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          %s
                          %s
                        </tr>
                        <tr>
                          %s
                          %s
                        </tr>
                      </table>

                      <h2 style="font-size:18px; color:#1a202c; margin:30px 0 15px;">Horários</h2>

                      <table width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          %s
                          %s
                        </tr>
                        <tr>
                          %s
                          %s
                        </tr>
                      </table>

                      <h2 style="font-size:18px; color:#1a202c; margin:30px 0 15px;">Pagamentos</h2>

                      <table width="100%%" cellspacing="0" cellpadding="0">
                        <tr>
                          %s
                          %s
                        </tr>
                        <tr>
                          %s
                          <td></td>
                        </tr>
                      </table>

                      <h2 style="font-size:18px; color:#1a202c; margin:30px 0 15px;">Ranking de especialidades</h2>

                      %s

                    </td>
                  </tr>

                  <tr>
                    <td style="padding:18px 35px; background:#f8fafc; border-top:1px solid #edf2f7; text-align:center; color:#8a94a6; font-size:13px;">
                      Este é um e-mail automático, por favor não responda.
                    </td>
                  </tr>

                </table>

                <p style="font-size:12px; color:#a0aec0; margin-top:18px;">
                  © %d Consulta Médica Online. Todos os direitos reservados.
                </p>

              </td>
            </tr>
          </table>

        </body>
        </html>
        """.formatted(
                ini.format(DATA_BR),
                fim.format(DATA_BR),

                card("Total de usuários", kpis.totalUsuarios()),
                card("Administradores", kpis.totalAdmins()),
                card("Colaboradores", kpis.totalColaboradores()),
                card("Usuários normais", kpis.totalNormais()),

                card("Total no período", kpis.totalHorariosPeriodo()),
                card("Livres", kpis.horariosLivresPeriodo()),
                card("Marcados", kpis.horariosMarcadosPeriodo()),
                card("Pagos", kpis.horariosPagosPeriodo()),

                card("Pagamentos criados", kpis.pagamentosCriadosPeriodo()),
                card("Pagamentos pagos", kpis.pagamentosPagosPeriodo()),
                card("Total pago", MOEDA_BR.format(totalPago)),

                rankingHtml,
                Year.now().getValue()
        );
    }

    private String card(String titulo, Object valor) {
        return """
        <td width="50%%" style="padding:6px;">
          <div style="background:#f8fafc; border:1px solid #edf2f7; border-radius:10px; padding:16px;">
            <div style="font-size:13px; color:#718096; margin-bottom:6px;">%s</div>
            <div style="font-size:22px; font-weight:700; color:#1a202c;">%s</div>
          </div>
        </td>
        """.formatted(titulo, valor);
    }

    private String montarRankingHtml(List<EspecialidadeQtdDTO> ranking) {

        if (ranking == null || ranking.isEmpty()) {
            return """
            <div style="background:#f8fafc; border:1px solid #edf2f7; border-radius:10px; padding:16px; color:#718096;">
              Nenhuma especialidade encontrada.
            </div>
            """;
        }

        StringBuilder sb = new StringBuilder();

        sb.append("""
        <table width="100%%" cellspacing="0" cellpadding="0" style="border-collapse:collapse; border:1px solid #edf2f7; border-radius:10px; overflow:hidden;">
          <tr style="background:#f8fafc;">
            <th align="left" style="padding:12px; color:#4a5568; font-size:13px;">Especialidade</th>
            <th align="right" style="padding:12px; color:#4a5568; font-size:13px;">Quantidade</th>
          </tr>
        """);

        for (EspecialidadeQtdDTO item : ranking) {
            sb.append("""
              <tr>
                <td style="padding:12px; border-top:1px solid #edf2f7; color:#1a202c; font-size:14px;">%s</td>
                <td align="right" style="padding:12px; border-top:1px solid #edf2f7; color:#1a202c; font-size:14px; font-weight:600;">%s</td>
              </tr>
            """.formatted(item.especialidade(), item.qtd()));
        }

        sb.append("</table>");

        return sb.toString();
    }
}