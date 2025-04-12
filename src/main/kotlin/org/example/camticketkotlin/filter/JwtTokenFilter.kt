package org.example.camticket.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.example.camticket.domain.User;
import org.example.camticket.exception.DoNotLoginException;
import org.example.camticket.exception.WrongTokenException;
import org.example.camticket.service.AuthService;
import org.example.camticket.util.JwtUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtTokenFilter extends OncePerRequestFilter {
    private final AuthService authService;
    private final String SECRET_KEY;
    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        if (uri.startsWith("/error") ||
                uri.startsWith("/camticket/auth/") ||
                uri.startsWith("/camticket/every") ||
                uri.equals("/")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authorizationHeader == null) {
            String paramToken = request.getParameter("token");
            if (paramToken == null) {
                throw new DoNotLoginException();
            }
            processAccessToken(request, response, filterChain, paramToken);
            return;
        }

        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new WrongTokenException("Bearer 로 시작하지 않는 토큰입니다.");
        }

        String token = authorizationHeader.split(" ")[1];
        processAccessToken(request, response, filterChain, token);
    }

    private void processAccessToken(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain, String token)
            throws ServletException, IOException {
        User loginUser = authService.getLoginUser(JwtUtil.getUserId(token, SECRET_KEY));
        setAuthenticationForUser(request, loginUser);
        filterChain.doFilter(request, response);
    }

    private void setAuthenticationForUser(HttpServletRequest request, User user) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        List.of(new SimpleGrantedAuthority(user.getRole().name()))
                );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
