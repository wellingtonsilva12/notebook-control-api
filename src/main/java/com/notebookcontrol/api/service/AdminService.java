package com.notebookcontrol.api.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controle de acesso do administrador.
 * A senha vem da variável de ambiente ADMIN_PASSWORD (configurada no Render).
 * O login devolve um token assinado (HMAC) que vale por 8 horas.
 */
@Service
public class AdminService {

    private static final long TTL_MS = 8L * 60 * 60 * 1000;
    private static final int MAX_TENTATIVAS = 5;
    private static final long JANELA_MS = 60_000;

    private final String senha = System.getenv("ADMIN_PASSWORD");
    private final Map<String, long[]> tentativas = new ConcurrentHashMap<>(); // ip -> [contagem, inicioJanela]

    public boolean configurada() {
        return senha != null && !senha.isBlank();
    }

    public String login(String senhaInformada, String ip) {
        if (!configurada()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Senha de administrador não configurada no servidor (variável ADMIN_PASSWORD).");
        }
        verificarLimite(ip);
        boolean ok = senhaInformada != null && MessageDigest.isEqual(
                sha256(senhaInformada), sha256(senha));
        if (!ok) {
            registrarFalha(ip);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Senha incorreta.");
        }
        tentativas.remove(ip);
        long expira = System.currentTimeMillis() + TTL_MS;
        return expira + "." + assinar(Long.toString(expira));
    }

    public void exigirAdmin(String token) {
        if (!configurada() || token == null) throw negado();
        int ponto = token.indexOf('.');
        if (ponto < 1) throw negado();
        String expiraStr = token.substring(0, ponto);
        String assinatura = token.substring(ponto + 1);
        long expira;
        try {
            expira = Long.parseLong(expiraStr);
        } catch (NumberFormatException e) {
            throw negado();
        }
        boolean assinaturaOk = MessageDigest.isEqual(
                assinar(expiraStr).getBytes(StandardCharsets.UTF_8),
                assinatura.getBytes(StandardCharsets.UTF_8));
        if (!assinaturaOk || expira < System.currentTimeMillis()) throw negado();
    }

    private ApiException negado() {
        return new ApiException(HttpStatus.UNAUTHORIZED, "Acesso restrito ao administrador.");
    }

    private void verificarLimite(String ip) {
        long[] t = tentativas.get(ip);
        long agora = System.currentTimeMillis();
        if (t != null && agora - t[1] < JANELA_MS && t[0] >= MAX_TENTATIVAS) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS,
                    "Muitas tentativas. Aguarde 1 minuto e tente de novo.");
        }
    }

    private void registrarFalha(String ip) {
        long agora = System.currentTimeMillis();
        tentativas.compute(ip, (k, t) -> {
            if (t == null || agora - t[1] >= JANELA_MS) return new long[]{1, agora};
            t[0]++;
            return t;
        });
    }

    private byte[] sha256(String s) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String assinar(String dado) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(sha256("atlas-admin:" + senha), "HmacSHA256"));
            byte[] h = mac.doFinal(dado.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
