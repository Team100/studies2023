package org.team100.lib.estimator;

import java.util.function.BiFunction;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.Matrix;
import edu.wpi.first.math.Nat;
import edu.wpi.first.math.StateSpaceUtil;
import edu.wpi.first.math.VecBuilder;
import edu.wpi.first.math.estimator.AngleStatistics;
import edu.wpi.first.math.estimator.ExtendedKalmanFilter;
import edu.wpi.first.math.numbers.N1;
import edu.wpi.first.math.numbers.N2;

/**
 * For testing LQR with angle wrapping.
 * 
 * Has an EKF inside.
 * 
 * The model for this test is a 1-DOF arm without gravity.
 * 
 * state: (angle, angular velocity)
 * measurement: (angle, angular velocity) (note these are never updated
 * together)
 * output: torque, i guess?
 */
public class ExtendedAngleEstimator {
    private final ExtendedKalmanFilter<N2, N1, N2> ekf;

    /**
     * Measurement variances.
     */
    private final Matrix<N2, N2> contR;
    private final Matrix<N1, N1> RAngle;
    private final Matrix<N1, N1> RVelocity;

    /**
     * The derivative of state.
     * 
     * x = (position, velocity)
     * xdot = (velocity, control)
     */
    private static Matrix<N2, N1> f(Matrix<N2, N1> x, Matrix<N1, N1> u) {
        return VecBuilder.fill(x.get(1, 0), u.get(0, 0));
    }

    /**
     * Both measurements: (position, velocity). u-invariant.
     */
    private static Matrix<N2, N1> h(Matrix<N2, N1> x, Matrix<N1, N1> u) {
        return x;
    }

    /**
     * Measures angular position. u-invariant.
     */
    private static Matrix<N1, N1> hPosition(Matrix<N2, N1> x, Matrix<N1, N1> u) {
        return VecBuilder.fill(x.get(0, 0));
    }

    /**
     * Measures angular velocity. u-invariant.
     */
    private static Matrix<N1, N1> hVelocity(Matrix<N2, N1> x, Matrix<N1, N1> u) {
        return VecBuilder.fill(x.get(1, 0));
    }

    /**
     * @param f                  system dynamics, must be control-affine
     * @param h                  measurement, must be u-invariant (TODO: enforce)
     * @param measurementStdDevs vector of std deviations per measurement
     */
    public ExtendedAngleEstimator(
            BiFunction<Matrix<N2, N1>, Matrix<N1, N1>, Matrix<N2, N1>> f,
            BiFunction<Matrix<N2, N1>, Matrix<N1, N1>, Matrix<N2, N1>> h,
            Matrix<N2, N1> stateStdDevs,
            Matrix<N2, N1> measurementStdDevs,
            double dtSeconds) {
        ekf = new ExtendedKalmanFilter<N2, N1, N2>(
                Nat.N2(),
                Nat.N1(),
                Nat.N2(),
                f,
                h,
                stateStdDevs,
                measurementStdDevs,
                AngleStatistics.angleResidual(0),
                AngleStatistics.angleAdd(0),
                dtSeconds);
        contR = StateSpaceUtil.makeCovarianceMatrix(Nat.N2(), measurementStdDevs);
        RAngle = contR.block(Nat.N1(), Nat.N1(), 0, 0);
        RVelocity = contR.block(Nat.N1(), Nat.N1(), 1, 1);
    }

    /**
     * Predict state, wrapping the angle if required.
     * 
     * @param u     total control output
     * @param dtSec time quantum (sec)
     */
    public void predictState(double u, double dtSec) {
        ekf.predict(VecBuilder.fill(u), ExtendedAngleEstimator::f, dtSec);
        Matrix<N2, N1> xhat = ekf.getXhat();
        xhat.set(0, 0, MathUtil.angleModulus(xhat.get(0, 0)));
        ekf.setXhat(xhat);
    }

    /**
     * Update with specified position and zero u (because u doesn't affect state
     * updates)
     */
    public void correctAngle(double y) {
        ekf.correct(
                Nat.N1(),
                VecBuilder.fill(0),
                VecBuilder.fill(y),
                ExtendedAngleEstimator::hPosition,
                RAngle,
                AngleStatistics.angleResidual(0),
                AngleStatistics.angleAdd(0));
    }

    /**
     * Update with specified velocity and zero u (because u doesn't affect state
     * updates)
     */
    public void correctVelocity(double y) {
        ekf.correct(
                Nat.N1(),
                VecBuilder.fill(0),
                VecBuilder.fill(y),
                ExtendedAngleEstimator::hVelocity,
                RVelocity,
                Matrix::minus,
                AngleStatistics.angleAdd(0));
    }

    /**
     * Update with specified state and zero u (because u doesn't affect state
     * updates)
     */
    public void correctBoth(double angle, double velocity) {
        ekf.correct(
                Nat.N2(),
                VecBuilder.fill(0),
                VecBuilder.fill(angle, velocity),
                ExtendedAngleEstimator::h,
                contR,
                AngleStatistics.angleResidual(0),
                AngleStatistics.angleAdd(0));
    }

    public void reset() {
        ekf.reset();
    }

    public void setXhat(Matrix<N2, N1> xHat) {
        ekf.setXhat(xHat);
    }

    public double getXhat(int row) {
        return ekf.getXhat(row);
    }

    public double getP(int row, int col) {
        return ekf.getP(row, col);
    }

    public Matrix<N2, N1> getXhat() {
        return ekf.getXhat();
    }
}