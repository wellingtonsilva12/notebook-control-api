package com.notebookcontrol.api.service;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Regras de horário da faculdade (fuso America/Sao_Paulo).
 *
 *   segunda a sexta: aula 19:00–22:30 | retirada 19:00–19:30 | devolução a partir das 21:30
 *   sábado:          aula 09:30–13:00 | retirada 09:30–10:00 | devolução a partir das 12:00
 *   domingo:         fechado
 *
 * - RETIRADA: só nos primeiros 30 min da aula.
 * - DEVOLUÇÃO: bloqueada da abertura da aula até o início da janela de devolução; liberada nos últimos
 *   60 min e também fora do horário de aula (para quem ficou com o notebook).
 * - No meio da aula só o administrador movimenta (saída antecipada: professor usa "Confirmar" com comentário).
 *
 * Para desligar o bloqueio (testes): variável de ambiente BLOQUEIO_HORARIO=off.
 */
@Service
public class HorarioService {

    private static final ZoneId ZONA = ZoneId.of("America/Sao_Paulo");
    private static final String[] DIAS = {
            "segunda-feira", "terça-feira", "quarta-feira", "quinta-feira", "sexta-feira", "sábado", "domingo"};

    private final boolean ativo = !"off".equalsIgnoreCase(System.getenv("BLOQUEIO_HORARIO"));

    /** {início da aula, fim da retirada, início da devolução, fim da aula} ou null se fechado. */
    private LocalTime[] janela(DayOfWeek dia) {
        if (dia == DayOfWeek.SATURDAY) {
            return new LocalTime[]{LocalTime.of(9, 30), LocalTime.of(10, 0), LocalTime.of(12, 0), LocalTime.of(13, 0)};
        }
        if (dia == DayOfWeek.SUNDAY) return null;
        return new LocalTime[]{LocalTime.of(19, 0), LocalTime.of(19, 30), LocalTime.of(21, 30), LocalTime.of(22, 30)};
    }

    public boolean retiradaAberta() {
        return retiradaAberta(ZonedDateTime.now(ZONA));
    }

    public boolean retiradaAberta(ZonedDateTime agora) {
        if (!ativo) return true;
        LocalTime[] j = janela(agora.getDayOfWeek());
        if (j == null) return false;
        LocalTime t = agora.toLocalTime();
        return !t.isBefore(j[0]) && t.isBefore(j[1]);
    }

    public boolean devolucaoAberta() {
        return devolucaoAberta(ZonedDateTime.now(ZONA));
    }

    public boolean devolucaoAberta(ZonedDateTime agora) {
        if (!ativo) return true;
        LocalTime[] j = janela(agora.getDayOfWeek());
        if (j == null) return true;
        LocalTime t = agora.toLocalTime();
        boolean meioDaAula = !t.isBefore(j[0]) && t.isBefore(j[2]);
        return !meioDaAula;
    }

    /** Próxima abertura de retirada, ex.: "hoje às 19:00", "amanhã às 09:30", "segunda-feira às 19:00". */
    public String proximaRetirada(ZonedDateTime agora) {
        LocalDate hoje = agora.toLocalDate();
        for (int i = 0; i <= 7; i++) {
            LocalDate dia = hoje.plusDays(i);
            LocalTime[] j = janela(dia.getDayOfWeek());
            if (j == null) continue;
            if (i == 0 && !agora.toLocalTime().isBefore(j[0])) continue;
            String quando = i == 0 ? "hoje" : (i == 1 ? "amanhã" : DIAS[dia.getDayOfWeek().getValue() - 1]);
            return quando + " às " + j[0];
        }
        return "segunda-feira às 19:00";
    }

    public String mensagemRetirada() {
        return "Retiradas só nos primeiros 30 minutos da aula (seg a sex 19:00–19:30, sábado 09:30–10:00). "
                + "Próxima abertura: " + proximaRetirada(ZonedDateTime.now(ZONA)) + ". "
                + "Se precisar retirar agora, peça ao professor (administrador).";
    }

    public String mensagemDevolucao() {
        return "Durante a aula só o administrador movimenta. Devoluções liberadas nos últimos 60 minutos "
                + "(seg a sex a partir das 21:30, sábado a partir das 12:00). "
                + "Se precisar sair antes, peça ao professor (administrador).";
    }
}
