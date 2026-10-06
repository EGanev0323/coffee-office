package bg.office.coffee.security;

import bg.office.coffee.repo.AppUserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Чете Bearer токена и зарежда потребителя от базата при всяка заявка,
 * така че деактивиран профил или сменена роля важат веднага.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository users;

    public JwtAuthFilter(JwtService jwtService, AppUserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
                        try {
                TokenClaims claims = jwtService.parse(header.substring(7));
                users.findById(claims.userId())
                        .filter(u -> u.isActive() && u.getTokenVersion() == claims.tokenVersion())
                        .ifPresent(u -> {
                            var principal = new AuthUser(u.getId(), u.getUsername(), u.getRole(), claims.remember());
                            var auth = new UsernamePasswordAuthenticationToken(
                                    principal, null,
                                    List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                            SecurityContextHolder.getContext().setAuthentication(auth);
                        });
            } catch (JwtException | IllegalArgumentException ignored) {
                // Невалиден или изтекъл токен – заявката продължава като анонимна и ще получи 401.
            }
        }
        chain.doFilter(request, response);
    }
}
