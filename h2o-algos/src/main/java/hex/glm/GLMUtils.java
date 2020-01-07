package hex.glm;

import water.MemoryManager;
import water.fvec.Frame;
import water.util.ArrayUtils;

import java.util.ArrayList;

public class GLMUtils {

  /***
   * From the gamColnames, this method attempts to translate to the column indices in adaptFrame.
   * @param adaptFrame
   * @param gamColnames
   * @return
   */
  public static int[][] extractAdaptedFrameIndices(Frame adaptFrame, String[][] gamColnames, int numOffset) {
    String[] frameNames = adaptFrame.names();
    ArrayList<String> allColNames = new ArrayList<>();
    for (String name:frameNames)
      allColNames.add(name);
    int[][] gamColIndices = new int[gamColnames.length][];
    int numFrame = gamColnames.length;
    for (int frameNum=0; frameNum < numFrame; frameNum++) {
      int numCols = gamColnames[frameNum].length;
      gamColIndices[frameNum] = MemoryManager.malloc4(numCols);
      for (int index=0; index < numCols; index++) {
        gamColIndices[frameNum][index] = numOffset+allColNames.indexOf(gamColnames[frameNum][index]);
      }
    }
    return gamColIndices;
  }

  public static void updateGradGam(double[] gradient, double[][][] penalty_mat, int[][] gamBetaIndices, double[] beta) { // update gradient due to gam smoothness constraint
    int numGamCol = gamBetaIndices.length; // number of predictors used for gam
    for (int gamColInd = 0; gamColInd < numGamCol; gamColInd++) { // update each gam col separately
      int numRow = gamBetaIndices[gamColInd].length;
      for (int rowInd = 0; rowInd < numRow; rowInd++) {
        double innerSBeta = ArrayUtils.innerProductPartial(beta, gamBetaIndices[gamColInd],
                penalty_mat[gamColInd][rowInd]);
        int currentBetaIndex = gamBetaIndices[gamColInd][rowInd];
        gradient[currentBetaIndex] += innerSBeta+beta[currentBetaIndex]*penalty_mat[gamColInd][rowInd][rowInd];
      }
    }
  }

  public static void updateGradGamMultinomial(double[][] gradient, double[][][] penaltyMat, int[][] gamBetaIndices,
                                              double[][] beta) {
    int numClass = beta[0].length;
    int numGamCol = gamBetaIndices.length;
    for (int classInd = 0; classInd < numClass; classInd++) {
      for (int gamInd = 0; gamInd < numGamCol; gamInd++) {
        int numKnots = gamBetaIndices[gamInd].length;
        for (int rowInd = 0; rowInd < numKnots; rowInd++) {
          int betaIndR = gamBetaIndices[gamInd][rowInd];
          double temp = 0.0;
          for (int colInd = 0; colInd < numKnots; colInd++) {
            int betaIndC = gamBetaIndices[gamInd][colInd];
            temp += penaltyMat[gamInd][rowInd][colInd]*beta[betaIndR][classInd]*beta[betaIndC][classInd];
          }
          gradient[betaIndR][classInd] += temp;
        }
      }
    }
  }

  public static double calSmoothNess(double[] beta, double[][][] penaltyMatrix, int[][] gamColIndices) {
    int numGamCols = gamColIndices.length;
    double smoothval = 0;
    for (int gamCol=0; gamCol < numGamCols; gamCol++) {
      smoothval += ArrayUtils.innerProductPartial(beta, gamColIndices[gamCol], 
              ArrayUtils.multArrVecPartial(penaltyMatrix[gamCol], beta, gamColIndices[gamCol]));
    }
    return smoothval;
  }

  /**
   * 
   * @param beta multinomial number of class by number of predictors
   * @param penaltyMatrix
   * @param gamColIndices
   * @return
   */
  public static double calSmoothNess(double[][] beta, double[][][] penaltyMatrix, int[][] gamColIndices) {
    int numClass = beta.length;
    double smoothval=0;
    for (int classInd=0; classInd < numClass; classInd++) {
      smoothval += calSmoothNess(beta[classInd], penaltyMatrix, gamColIndices);
    }
    return smoothval;
  }
}