package COMSETsystem;

public class ProbabilityMatrix {
    int Version;
    double[][] Matrix;

    public ProbabilityMatrix(double[][] matrix) {
        Version = 0;
        Matrix = matrix;
    }

    public void updateMatrix(int version, double[][] matrix) {
        Version = version;
        Matrix = matrix;
    }
}
