package tn.esprit.msprojectservice.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    // --- @Before : log avant l'exécution de chaque méthode service ---
    @Before("execution(* tn.esprit.msprojectservice.services.*.*(..))")
    public void logBefore(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String args = Arrays.toString(joinPoint.getArgs());

        logger.info("▶ APPEL — {}.{}() | Paramètres : {}", className, methodName, args);
    }

    // --- @After : log après l'exécution de chaque méthode service ---
    @After("execution(* tn.esprit.msprojectservice.services.*.*(..))")
    public void logAfter(JoinPoint joinPoint) {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();

        logger.info("✔ FIN — {}.{}() exécutée avec succès", className, methodName);
    }
}