package com.vclipper.processing.application.common;

import java.util.Optional;
import java.util.function.Function;

/**
 * Result pattern for handling success/failure without exceptions
 * Provides a clean way to handle business logic outcomes
 * 
 * Use this for expected business conditions that are not exceptional:
 * - High-frequency business state checks (e.g., "video not ready")
 * - User-facing validation results
 * - Operations where "failure" is normal flow
 * 
 * Continue using exceptions for:
 * - Security boundaries (VideoNotFoundException)
 * - System errors (IOException, database failures)
 * - Rare error conditions
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    /**
     * Create a successful result
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    /**
     * Create a failed result
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    /**
     * Check if result is successful
     */
    boolean isSuccess();
    
    /**
     * Check if result is failure
     */
    boolean isFailure();
    
    /**
     * Get success value (empty if failure)
     */
    Optional<T> getValue();
    
    /**
     * Get error value (empty if success)
     */
    Optional<E> getError();
    
    /**
     * Map success value to another type
     */
    <U> Result<U, E> map(Function<T, U> mapper);
    
    /**
     * Map error value to another type
     */
    <F> Result<T, F> mapError(Function<E, F> mapper);
    
    /**
     * Success result
     */
    record Success<T, E>(T value) implements Result<T, E> {
        @Override
        public boolean isSuccess() {
            return true;
        }
        
        @Override
        public boolean isFailure() {
            return false;
        }
        
        @Override
        public Optional<T> getValue() {
            return Optional.of(value);
        }
        
        @Override
        public Optional<E> getError() {
            return Optional.empty();
        }
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return new Success<>(mapper.apply(value));
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> mapper) {
            return new Success<>(value);
        }
    }
    
    /**
     * Failure result
     */
    record Failure<T, E>(E error) implements Result<T, E> {
        @Override
        public boolean isSuccess() {
            return false;
        }
        
        @Override
        public boolean isFailure() {
            return true;
        }
        
        @Override
        public Optional<T> getValue() {
            return Optional.empty();
        }
        
        @Override
        public Optional<E> getError() {
            return Optional.of(error);
        }
        
        @Override
        public <U> Result<U, E> map(Function<T, U> mapper) {
            return new Failure<>(error);
        }
        
        @Override
        public <F> Result<T, F> mapError(Function<E, F> mapper) {
            return new Failure<>(mapper.apply(error));
        }
    }
}
