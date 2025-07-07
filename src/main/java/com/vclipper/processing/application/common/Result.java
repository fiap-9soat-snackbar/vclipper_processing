package com.vclipper.processing.application.common;

import java.util.Optional;
import java.util.function.Function;

/**
 * Result pattern implementation for type-safe error handling.
 * 
 * Follows the exact same pattern as vclipping's Result class for consistency.
 * Provides a functional approach to handling success/failure scenarios
 * without throwing exceptions in business logic.
 * 
 * @param <T> Success value type
 * @param <E> Error type
 */
public sealed interface Result<T, E> permits Result.Success, Result.Failure {
    
    /**
     * Check if this result represents a successful operation.
     * 
     * @return true if operation was successful
     */
    boolean isSuccess();
    
    /**
     * Check if this result represents a failed operation.
     * 
     * @return true if operation failed
     */
    boolean isFailure();
    
    /**
     * Get the success value if present.
     * 
     * @return Optional containing the success value
     */
    Optional<T> getValue();
    
    /**
     * Get the error if present.
     * 
     * @return Optional containing the error
     */
    Optional<E> getError();
    
    /**
     * Transform the success value if present.
     * 
     * @param mapper Function to transform the success value
     * @param <U> New success value type
     * @return New Result with transformed value or original error
     */
    <U> Result<U, E> map(Function<T, U> mapper);
    
    /**
     * Transform the success value to another Result.
     * 
     * @param mapper Function to transform the success value to Result
     * @param <U> New success value type
     * @return New Result from transformation or original error
     */
    <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper);
    
    // Factory methods
    
    /**
     * Create a successful result.
     * 
     * @param value Success value
     * @param <T> Success value type
     * @param <E> Error type
     * @return Success result
     */
    static <T, E> Result<T, E> success(T value) {
        return new Success<>(value);
    }
    
    /**
     * Create a failed result.
     * 
     * @param error Error value
     * @param <T> Success value type
     * @param <E> Error type
     * @return Failure result
     */
    static <T, E> Result<T, E> failure(E error) {
        return new Failure<>(error);
    }
    
    // Implementation classes
    
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
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return mapper.apply(value);
        }
    }
    
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
        public <U> Result<U, E> flatMap(Function<T, Result<U, E>> mapper) {
            return new Failure<>(error);
        }
    }
}
