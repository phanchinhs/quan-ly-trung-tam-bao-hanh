package com.example.btl.config;

import com.example.btl.entity.AuditLog;
import com.example.btl.service.AuditLogService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

@Aspect
@Component
public class AuditLogAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditLogAspect.class);
    private final AuditLogService auditLogService;

    public AuditLogAspect(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Around("@annotation(com.example.btl.config.Auditable)")
    public Object audit(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature sig = (MethodSignature) joinPoint.getSignature();
        Method method = sig.getMethod();
        Auditable auditable = method.getAnnotation(Auditable.class);

        String action = auditable.action();
        String entityName = auditable.entity();
        String description = auditable.description();
        String username = currentUsername();

        Object[] args = joinPoint.getArgs();
        Long beforeId = extractEntityId(args, method);

        Object result = joinPoint.proceed();

        Long entityId = beforeId;
        if (entityId == null && result != null) {
            entityId = extractIdFromResult(result);
        }
        if ("SAVE".equals(action)) {
            action = entityId != null ? "UPDATE" : "CREATE";
        }

        String details = description.isEmpty() ? action + " " + entityName : description;
        details = interpolate(details, args, entityId, username);
        try {
            auditLogService.log(action, entityName, entityId, username, details);
        } catch (Exception e) {
            log.warn("Không thể ghi audit log: {}", e.getMessage());
        }

        return result;
    }

    private String interpolate(String template, Object[] args, Long entityId, String username) {
        String s = template.replace("#{id}", entityId != null ? String.valueOf(entityId) : "?");
        s = s.replace("{user}", username);
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            s = replaceGetter(s, "name", arg);
            s = replaceGetter(s, "code", arg);
            s = replaceGetter(s, "phone", arg);
            s = replaceGetter(s, "serialNumber", arg);
        }
        return s;
    }

    private String replaceGetter(String template, String field, Object obj) {
        String token = "{" + field + "}";
        if (!template.contains(token)) {
            return template;
        }
        try {
            String getter = "get" + Character.toUpperCase(field.charAt(0)) + field.substring(1);
            Method m = obj.getClass().getMethod(getter);
            Object value = m.invoke(obj);
            if (value != null && !String.valueOf(value).isBlank()) {
                return template.replace(token, String.valueOf(value));
            }
        } catch (Exception ignored) {
        }
        return template;
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            return auth.getName();
        }
        return "system";
    }

    private Long extractEntityId(Object[] args, Method method) {
        Parameter[] params = method.getParameters();
        for (int i = 0; i < params.length; i++) {
            if (params[i].getType() == Long.class || params[i].getType() == long.class) {
                Object val = args[i];
                if (val != null) {
                    return ((Number) val).longValue();
                }
            }
        }
        for (Object arg : args) {
            if (arg != null) {
                try {
                    Method getId = arg.getClass().getMethod("getId");
                    Object id = getId.invoke(arg);
                    if (id != null) {
                        return ((Number) id).longValue();
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    private Long extractIdFromResult(Object result) {
        try {
            Method getId = result.getClass().getMethod("getId");
            Object id = getId.invoke(result);
            if (id != null) {
                return ((Number) id).longValue();
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
