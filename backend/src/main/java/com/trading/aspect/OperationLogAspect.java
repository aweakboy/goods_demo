package com.trading.aspect;

import com.trading.annotation.OperationLog;
import com.trading.entity.User;
import com.trading.service.OperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogService operationLogService;

    @AfterReturning(pointcut = "@annotation(operationLog)", returning = "returnValue")
    public void afterReturning(JoinPoint joinPoint, OperationLog operationLog, Object returnValue) {
        // Skip when annotated method signals failure (e.g. handleNotify returning false)
        if (Boolean.FALSE.equals(returnValue)) {
            return;
        }

        Long userId = null;
        String username = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof User user) {
            userId = user.getId();
            username = user.getUsername();
        }

        String resourceId = extractResourceId(returnValue);
        String ip = getClientIp();

        operationLogService.saveAsync(userId, username, operationLog.module(), operationLog.action(),
                resourceId, null, ip);
    }

    private String extractResourceId(Object returnValue) {
        if (returnValue == null) return null;
        try {
            return returnValue.getClass().getMethod("getId").invoke(returnValue).toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String getClientIp() {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return null;
            HttpServletRequest request = attrs.getRequest();
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                return xff.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return null;
        }
    }
}
