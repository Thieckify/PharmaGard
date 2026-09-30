package com.pharmagarde.backend.infrastructure.ai;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Limite le nombre d'appels à POST /api/assistant par adresse IP, pour protéger l'unique
 * instance Ollama (qui tourne sur CPU, donc lente) d'un usage en boucle sur cette route
 * publique et sans authentification. En mémoire uniquement, sans dépendance externe (Redis...) :
 * suffisant pour une seule instance de l'application, ce qui est le cas ici.
 * Limite volontairement large pour un usage humain normal (10/min), pensée pour bloquer un
 * script, pas pour gêner un visiteur qui tape plusieurs questions à la suite.
 */
@Component
public class RateLimitFilter extends HttpFilter {

    private static final int LIMITE_PAR_MINUTE = 10;
    private static final long FENETRE_MS = 60_000;

    // Horodatages des appels récents, par IP. ConcurrentLinkedDeque : sûr en accès concurrent
    // sans verrou explicite, adapté à de petits volumes (peu d'IP distinctes en pratique).
    private final ConcurrentHashMap<String, Deque<Long>> appelsParIp = new ConcurrentHashMap<>();

    @Override
    protected void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        boolean estRouteAssistant = "POST".equalsIgnoreCase(request.getMethod())
                && "/api/assistant".equals(request.getRequestURI());

        if (!estRouteAssistant || autoriser(request.getRemoteAddr())) {
            chain.doFilter(request, response);
            return;
        }

        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("""
                {"status":429,"error":"Too Many Requests","message":"Trop de questions envoyées, réessayez dans une minute"}""");
    }

    private boolean autoriser(String ip) {
        long maintenant = Instant.now().toEpochMilli();
        Deque<Long> appels = appelsParIp.computeIfAbsent(ip, cle -> new ConcurrentLinkedDeque<>());

        // Retire les appels sortis de la fenêtre d'une minute
        while (!appels.isEmpty() && maintenant - appels.peekFirst() > FENETRE_MS) {
            appels.pollFirst();
        }

        if (appels.size() >= LIMITE_PAR_MINUTE) {
            return false;
        }

        appels.addLast(maintenant);
        return true;
    }
}