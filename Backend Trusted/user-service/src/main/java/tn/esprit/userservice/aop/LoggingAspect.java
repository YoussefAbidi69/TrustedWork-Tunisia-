package tn.esprit.userservice.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    @Before("execution(* tn.esprit.userservice.controller..*(..)) || execution(* tn.esprit.userservice.service..*(..))")
    public void logBefore(JoinPoint joinPoint) {
        log.info("Entering: {}.{}() with arguments = {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                Arrays.toString(joinPoint.getArgs()));
    }

    @AfterReturning(
            pointcut = "execution(* tn.esprit.userservice.controller..*(..)) || execution(* tn.esprit.userservice.service..*(..))",
            returning = "result"
    )
    public void logAfterReturning(JoinPoint joinPoint, Object result) {
        log.info("Exiting: {}.{}() with result = {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                result);
    }

    @AfterThrowing(
            pointcut = "execution(* tn.esprit.userservice.controller..*(..)) || execution(* tn.esprit.userservice.service..*(..))",
            throwing = "exception"
    )
    public void logAfterThrowing(JoinPoint joinPoint, Exception exception) {
        log.error("Exception in: {}.{}() with cause = {} and message = {}",
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName(),
                exception.getCause() != null ? exception.getCause() : "NULL",
                exception.getMessage());
    }
}