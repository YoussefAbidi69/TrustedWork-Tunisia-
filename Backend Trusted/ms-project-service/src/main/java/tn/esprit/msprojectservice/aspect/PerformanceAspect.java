package tn.esprit.msprojectservice.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class PerformanceAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceAspect.class);

    // --- @Around : mesurer le temps d'exécution de chaque méthode service ---
    @Around("execution(* tn.esprit.msprojectservice.services.*.*(..))")
    public Object measureExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        long startTime = System.currentTimeMillis();

        // Exécuter la méthode
        Object result = joinPoint.proceed();

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Log avec seuil d'alerte
        if (duration > 1000) {
            logger.warn("⏱ LENT — {}.{}() a pris {} ms (> 1 seconde !)", className, methodName, duration);
        } else {
            logger.info("⏱ PERF — {}.{}() exécutée en {} ms", className, methodName, duration);
        }

        return result;
    }
}