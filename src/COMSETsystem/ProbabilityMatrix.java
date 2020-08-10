package COMSETsystem;

public class ProbabilityMatrix {
    public int Version;
    public double[][] Matrix;

    public ProbabilityMatrix(double[][] matrix) {
        Version = 0;
        Matrix = matrix;
    }

    public void updateMatrix(double[][] matrix) {
        Version++;
        Matrix = matrix;
    }
}
