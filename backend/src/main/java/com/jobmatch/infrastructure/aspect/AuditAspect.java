package com.jobmatch.infrastructure.aspect;

import com.jobmatch.domain.entity.AuditLog;
import com.jobmatch.domain.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @Pointcut("execution(* com.jobmatch.service.impl.AuthServiceImpl.login(..))")
    public void loginPointcut() {}

    @Pointcut("execution(* com.jobmatch.service.impl.AuthServiceImpl.register(..))")
    public void registerPointcut() {}

    @Pointcut("execution(* com.jobmatch.service.impl.JobServiceImpl.createJob(..))")
    public void createJobPointcut() {}

    @Pointcut("execution(* com.jobmatch.service.impl.JobServiceImpl.deleteJob(..))")
    public void deleteJobPointcut() {}

    @Pointcut("execution(* com.jobmatch.service.impl.UserServiceImpl.uploadResume(..))")
    public void uploadResumePointcut() {}

    @Pointcut("execution(* com.jobmatch.service.impl.UserServiceImpl.updateProfile(..))")
    public void updateProfilePointcut() {}

    @Pointcut("execution(* com.jobmatch.service.impl.ApplicationServiceImpl.applyToJob(..))")
    public void applyToJobPointcut() {}

    @AfterReturning("loginPointcut()")
    public void auditLogin(JoinPoint joinPoint) {
        logAudit("LOGIN", "Auth", null, null);
    }

    @AfterReturning("registerPointcut()")
    public void auditRegister(JoinPoint joinPoint) {
        logAudit("REGISTER", "Auth", null, null);
    }

    @AfterReturning(pointcut = "createJobPointcut()", returning = "result")
    public void auditCreateJob(JoinPoint joinPoint, Object result) {
        Long jobId = extractId(result);
        logAudit("JOB_CREATED", "Job", jobId, null);
    }

    @AfterReturning("deleteJobPointcut()")
    public void auditDeleteJob(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Long jobId = args.length > 0 ? (Long) args[0] : null;
        logAudit("JOB_DELETED", "Job", jobId, null);
    }

    @AfterReturning("uploadResumePointcut()")
    public void auditUploadResume(JoinPoint joinPoint) {
        logAudit("RESUME_UPLOADED", "User", null, null);
    }

    @AfterReturning("updateProfilePointcut()")
    public void auditUpdateProfile(JoinPoint joinPoint) {
        logAudit("PROFILE_UPDATED", "User", null, null);
    }

    @AfterReturning("applyToJobPointcut()")
    public void auditApplyToJob(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        Long jobId = args.length > 0 ? (Long) args[1] : null;
        logAudit("APPLICATION_SUBMITTED", "Application", jobId, null);
    }

    private void logAudit(String action, String entityType, Long entityId, String details) {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            
            AuditLog auditLog = new AuditLog();
            
            // Try to get the current user ID from the request attribute (set by JWT filter)
            Object userIdAttr = request.getAttribute("currentUserId");
            if (userIdAttr instanceof Long) {
                auditLog.setUserId((Long) userIdAttr);
            }
            
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setIpAddress(getClientIp(request));
            auditLog.setUserAgent(request.getHeader("User-Agent"));
            auditLog.setDetails(details);
            
            auditLogRepository.save(auditLog);
            log.debug("Audit log saved: {} - {}", action, entityType);
        } catch (Exception e) {
            log.warn("Failed to save audit log: {}", e.getMessage());
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank()) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    private Long extractId(Object result) {
        if (result == null) return null;
        try {
            return (Long) result.getClass().getMethod("getId").invoke(result);
        } catch (Exception e) {
            try {
                return (Long) result.getClass().getMethod("id").invoke(result);
            } catch (Exception ex) {
                return null;
            }
        }
    }
}