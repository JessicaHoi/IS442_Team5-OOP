package is442t1.studybuddy.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import is442t1.studybuddy.common.exception.AuthenticationException;
import is442t1.studybuddy.common.exception.PermissionDeniedException;

/**
 * Guards the API: every request needs a valid bearer token, and handlers
 * marked with {@link RequireRole} also need the matching role.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /** Request attribute holding the {@link AuthenticatedUser}. */
    static final String CURRENT_USER_ATTRIBUTE = AuthInterceptor.class.getName() + ".currentUser";

    private final SessionTokenService tokenService;

    public AuthInterceptor(SessionTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }
        AuthenticatedUser user = BearerToken.extract(request.getHeader(HttpHeaders.AUTHORIZATION))
                .flatMap(tokenService::resolve)
                .orElseThrow(() -> new AuthenticationException("Please sign in to continue."));

        RequireRole required = requiredRole(handlerMethod);
        if (required != null && required.value() != user.role()) {
            throw new PermissionDeniedException("You do not have permission to do that.");
        }
        request.setAttribute(CURRENT_USER_ATTRIBUTE, user);
        return true;
    }

    private static RequireRole requiredRole(HandlerMethod handlerMethod) {
        RequireRole onMethod = handlerMethod.getMethodAnnotation(RequireRole.class);
        return onMethod != null ? onMethod : handlerMethod.getBeanType().getAnnotation(RequireRole.class);
    }
}
