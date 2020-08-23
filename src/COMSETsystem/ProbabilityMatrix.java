package COMSETsystem;

import java.util.Arrays;

public class ProbabilityMatrix {
    public int Version;
    public double[][] Matrix;

    public ProbabilityMatrix(int n) {
        Version = 0;
        Matrix = new double[n][n];
        for (int i=0; i<n; i++) {
            Arrays.fill(Matrix[i], 1f/n);
        }
    }

    public void updateMatrix(double[][] matrix) {
        Version++;
        Matrix = matrix;
    }
}
