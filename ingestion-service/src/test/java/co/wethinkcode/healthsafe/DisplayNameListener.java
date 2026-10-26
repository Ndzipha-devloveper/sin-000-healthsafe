// package co.wethinkcode.healthsafe;

// import org.junit.platform.engine.TestExecutionResult;
// import org.junit.platform.launcher.TestExecutionListener;
// import org.junit.platform.launcher.TestIdentifier;

// public class DisplayNameListener implements TestExecutionListener {

//     @Override
//     public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult result) {
//         if (testIdentifier.isTest()) {
//             String status = result.getStatus() == TestExecutionResult.Status.SUCCESSFUL ? "PASS" : "FAIL";
//             System.out.println("  [" + status + "] " + testIdentifier.getDisplayName());
//         }
//     }
// }