/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util.function;

/**
 * 
 * @since 0.8
 * 
 * @author Haiyang Li
 * 
 * @see java.util.function.IntUnaryOperator
 */
// public interface DoubleUnaryOperator {
public interface DoubleUnaryOperator extends java.util.function.DoubleUnaryOperator {
    @Override
    double applyAsDouble(double operand);
}