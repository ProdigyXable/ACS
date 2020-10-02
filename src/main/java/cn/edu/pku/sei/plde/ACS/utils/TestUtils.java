package cn.edu.pku.sei.plde.ACS.utils;

import cn.edu.pku.sei.plde.ACS.fix.SuspiciousFixer;
import cn.edu.pku.sei.plde.ACS.junit.JunitRunner;
import cn.edu.pku.sei.plde.ACS.main.Config;
import com.gzoltar.core.GZoltar;
import com.gzoltar.core.instr.testing.TestResult;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javassist.NotFoundException;
import org.apache.commons.lang.StringUtils;
import utdallas.edu.profl.replicate.patchcategory.DefaultPatchCategories;
import utdallas.edu.profl.replicate.patchcategory.PatchCategory;
import utdallas.edu.profl.replicate.util.ProflResultRanking;
import utdallas.edu.profl.replicate.util.XiaMethodLineCoverage;
import utdallas.edu.profl.replicate.util.XiaTestLineCoverage;

/**
 * Created by yanrunfa on 16/3/8.
 */
public class TestUtils {

    public static int patchAttempts = 0;

    public static String proflPathFailingTest = "";
    public static String proflPathMethodLineCoverage = "";
    public static String proflPathTestLineCoverage = "";

    public static Map<String, Double> prr_map = new TreeMap();
    public static ProflResultRanking prr = null;

    /**
     *
     * @param classpath
     * @param testPath
     * @param classname
     * @return
     */
    public static String getTestTrace(List<String> classpath, String testPath, String classname, String functionname) throws NotFoundException {
        ArrayList<String> classpaths = new ArrayList<String>();
        for (String path : classpath) {
            classpaths.add(path);
        }
        classpaths.add(testPath);
        GZoltar gzoltar = null;
        try {
            gzoltar = new GZoltar(System.getProperty("user.dir"));
            gzoltar.setClassPaths(classpaths);
            gzoltar.addPackageNotToInstrument("org.junit");
            gzoltar.addPackageNotToInstrument("junit.framework");
            gzoltar.addTestPackageNotToExecute("junit.framework");
            gzoltar.addTestPackageNotToExecute("org.junit");
            gzoltar.addTestToExecute(classname);
            gzoltar.addClassNotToInstrument(classname);
            ExecutorService service = Executors.newSingleThreadExecutor();
            Future<Boolean> future = service.submit(new GzoltarRunProcess(gzoltar));
            try {
                future.get(Config.GZOLTAR_RUN_TIMEOUT, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                future.cancel(true);
                service.shutdownNow();
                RuntimeUtils.killProcess();
                e.printStackTrace();
                return "timeout";
            } catch (TimeoutException e) {
                future.cancel(true);
                service.shutdownNow();
                RuntimeUtils.killProcess();
                e.printStackTrace();
                return "timeout";
            } catch (ExecutionException e) {
                service.shutdownNow();
                future.cancel(true);
                RuntimeUtils.killProcess();
                return "timeout";
            }
        } catch (NullPointerException e) {
            throw new NotFoundException("Test Class " + classname + " No Found in Test Class Path " + testPath);
        } catch (IOException ex) {
            Logger.getLogger(TestUtils.class.getName()).log(Level.SEVERE, null, ex);
        }
        // catch (IOException e) {
        //    return "";
        // }
        List<TestResult> testResults = gzoltar.getTestResults();
        for (TestResult testResult : testResults) {
            if (testResult.getName().substring(testResult.getName().lastIndexOf('#') + 1).equals(functionname)) {
                return testResult.getTrace();
            }
        }
        throw new NotFoundException("No Test Named " + functionname + " Found in Test Class " + classname);
    }

    public static String getTestTrace(String classpath, String testPath, String classname, String functionname) throws NotFoundException {
        return getTestTrace(Arrays.asList(classpath), testPath, classname, functionname);
    }

    public static String getTestTraceFromJunit(String classpath, String testPath, String className, String methodName) {
        String[] arg = {"java", "-cp", buildClasspath(classpath, testPath, new ArrayList<String>(), new ArrayList<String>(Arrays.asList(PathUtils.getJunitPath()))), "JunitRunner", className + "#" + methodName};
        try {
            return ShellUtils.shellRun(Arrays.asList(StringUtils.join(arg, " ")));
        } catch (IOException e) {
            return null;
        }
    }

    public static String getTestTraceFromJunit(String classpath, String testPath, List<String> libPath, String className, String methodName) {

        String[] arg = {"java", "-cp", buildClasspath(classpath, testPath, libPath, new ArrayList<String>(Arrays.asList(PathUtils.getJunitPath()))), "JunitRunner", className + "#" + methodName};
        try {
            return ShellUtils.shellRun(Arrays.asList(StringUtils.join(arg, " ")));
        } catch (IOException e) {
            return null;
        }
    }

    public static String getDefects4jTestResult(String projectName) {
        try {
            String result = ShellUtils.shellRun(Arrays.asList("cd project\n", "cd " + projectName + "\n", "defects4j test"));
            return result;
        } catch (IOException e) {
            return "";
        }
    }

    public static Map<String, Integer> getFailTestNumInProject(String projectName) {
        System.out.println("Failing Tests Queried");
        TreeMap<String, Integer> mapResult = new TreeMap();

        String testResult = getDefects4jTestResult(projectName);
        if (testResult.equals("")) {//error occurs in run
            return new TreeMap(); // Integer.MAX_VALUE;
        }
        if (!testResult.contains("Failing tests:")) {
            return new TreeMap(); // Integer.MAX_VALUE;
        }

        boolean flag = false;

        for (String lineString : testResult.split("\n")) {
            if (lineString.contains("Failing tests:")) {
                flag = true;
                String testName = lineString.split(":")[0].trim();
                int errorNum = Integer.valueOf(lineString.split(":")[1].trim());
                mapResult.put(testName, errorNum);
            }

            if (flag && lineString.contains(" - ")) {
                String testName = lineString.split(" - ")[1].trim();
                mapResult.put(testName, 0);
            }
        }

        if (SuspiciousFixer.originallyFailingTests == null) {
            System.out.println("Gathering initial failing test results!");
            SuspiciousFixer.originallyFailingTests = new TreeMap<>(mapResult);
            try {
                prr = new ProflResultRanking(new XiaMethodLineCoverage(TestUtils.proflPathMethodLineCoverage), new XiaTestLineCoverage(TestUtils.proflPathTestLineCoverage), TestUtils.proflPathFailingTest);
                System.out.println("Initialization of profl structures success!");

                saveGenInfo(projectName);
                saveProflInfo(projectName);
                saveCatInfo(projectName);
            } catch (Exception ex) {
                System.err.println("Initialization of profl structures failed!");
                System.err.println(ex.getMessage());
                Logger.getLogger(TestUtils.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            Set<String> fp = new TreeSet();
            Set<String> pf = new TreeSet();
            Set<String> pp = new TreeSet();
            Set<String> ff = new TreeSet();

            for (String failedTest : SuspiciousFixer.originallyFailingTests.keySet()) {
                if (failedTest.contains("::")) {
                    if (mapResult.containsKey(failedTest)) {
                        ff.add(failedTest);
                        System.out.println(String.format("[Fail->Fail] %s", failedTest));
                    } else {
                        fp.add(failedTest);
                        System.out.println(String.format("[Fail->Pass] %s", failedTest));
                    }
                }
            }
            for (String failedTest : mapResult.keySet()) {
                if (failedTest.contains("::")) {
                    if (!SuspiciousFixer.originallyFailingTests.containsKey(failedTest)) {
                        pf.add(failedTest);
                        System.out.println(String.format("[Pass->Fail] %s", failedTest));
                    }
                }
            }

            if (prr != null) {
                PatchCategory pc;

                if (fp.size() > 0 && pf.size() == 0) {
                    if (ff.size() == 0) {
                        System.out.println("Full CleanFix detected");
                        pc = DefaultPatchCategories.CLEAN_FIX_FULL;
                        prr.addCategoryEntry(DefaultPatchCategories.CLEAN_FIX_FULL, prr_map);
                    } else {
                        System.out.println("Partial CleanFix detected");
                        pc = DefaultPatchCategories.CLEAN_FIX_PARTIAL;
                        prr.addCategoryEntry(DefaultPatchCategories.CLEAN_FIX_PARTIAL, prr_map);
                    }
                } else if (fp.size() > 0 && pf.size() == 0) {
                    if (ff.size() == 0) {
                        System.out.println("Full NoisyFix detected");
                        pc = DefaultPatchCategories.NOISY_FIX_FULL;
                        prr.addCategoryEntry(DefaultPatchCategories.NOISY_FIX_FULL, prr_map);
                    } else {
                        System.out.println("Partial NoisyFix detected");
                        pc = DefaultPatchCategories.NOISY_FIX_PARTIAL;
                        prr.addCategoryEntry(DefaultPatchCategories.NOISY_FIX_PARTIAL, prr_map);
                    }
                } else if (fp.size() == 0 && pf.size() == 0) {
                    System.out.println("NoneFix detected");
                    pc = DefaultPatchCategories.NONE_FIX;
                    prr.addCategoryEntry(DefaultPatchCategories.NONE_FIX, prr_map);
                } else if (fp.size() == 0 && pf.size() > 0) {
                    System.out.println("NegFix detected");
                    pc = DefaultPatchCategories.NEG_FIX;
                    prr.addCategoryEntry(DefaultPatchCategories.NEG_FIX, prr_map);
                } else {
                    pc = null;
                    System.out.println(String.format("Unknown fix detected ff=%d fp=%d pf=%d pp=???", ff, fp, pf));
                }

                prr_map = new TreeMap();

                try {
                    saveGenInfo(projectName);
                    saveProflInfo(projectName);
                    saveCatInfo(projectName);
                    saveTestInfo(projectName, ff, fp, pf, pc);
                } catch (IOException e) {
                    System.out.println("Failed to successfully write profl information");
                    System.out.println(e.getMessage());
                }
            } else {
                System.out.println("ProFL was not initialized");
            }
        }
        return mapResult;
    }

    private static void saveGenInfo(String project) throws IOException {
        File outputFile = new File(String.format("acs-output/%s/generalSusInfo.profl", project));
        outputFile.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, false))) {
            System.out.println(String.format("Saving sbfl output information to %s", outputFile.getAbsolutePath()));
            for (String s : TestUtils.prr.outputSbflSus()) {
                bw.write(s);
                bw.newLine();
            }
        } catch (Exception e) {
            System.out.println("Could not save file to " + outputFile.getAbsolutePath());
            System.out.println(e.getMessage());
        }
    }

    private static void saveProflInfo(String project) throws IOException {
        File outputFile = new File(String.format("acs-output/%s/aggregatedSusInfo.profl", project));
        outputFile.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, false))) {
            System.out.println(String.format("Saving profl output information to %s", outputFile.getAbsolutePath()));
            for (String s : TestUtils.prr.outputProflResults()) {
                bw.write(s);
                bw.newLine();
            }
        } catch (Exception e) {
            System.out.println("Could not save file to " + outputFile.getAbsolutePath());
            System.out.println(e.getMessage());
        }
    }

    private static void saveCatInfo(String project) throws IOException {
        File outputFile = new File(String.format("acs-output/%s/category_information.profl", project));
        outputFile.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, false))) {
            System.out.println(String.format("Saving profl category information to %s", outputFile.getAbsolutePath()));
            for (String s : TestUtils.prr.outputProflCatInfo()) {
                bw.write(s);
                bw.newLine();
            }
        } catch (Exception e) {
            System.out.println("Could not save file to " + outputFile.getAbsolutePath());
            System.out.println(e.getMessage());
        }
    }

    private static String buildClasspath(String classpath, String testClasspath, List<String> libPaths, List<String> additionalPath) {
        if (libPaths.size() != 0) {
            additionalPath = new ArrayList<>(additionalPath);
            additionalPath.addAll(libPaths);
        }
        String path = "\"";
        path += classpath;
        path += System.getProperty("path.separator");
        path += testClasspath;
        path += System.getProperty("path.separator");
        path += JunitRunner.class.getProtectionDomain().getCodeSource().getLocation().getFile();
        path += System.getProperty("path.separator");
        path += StringUtils.join(additionalPath, System.getProperty("path.separator"));
        path += "\"";
        return path;
    }

    private static void saveTestInfo(String project, Set<String> ff, Set<String> fp, Set<String> pf, PatchCategory pc) {

        File outputFile = new File(String.format("acs-output/%s/tests/%d.tests", project, TestUtils.patchAttempts));
        outputFile.getParentFile().mkdirs();

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(outputFile, false))) {
            System.out.println(String.format("Saving patch information to %s", outputFile.getAbsolutePath()));

            bw.write(String.format("Test validation results: ff=%d fp=%d pf=%d pp=???%n", ff.size(), fp.size(), pf.size()));
            bw.write(String.format("Patch Category: %s", pc.getCategoryName()));
            bw.write("--------------------\n");

            for (String s : ff) {
                bw.write(String.format("[Fail->Fail] %s", s));
                bw.newLine();
            }

            for (String s : fp) {
                bw.write(String.format("[Fail->Pass] %s", s));
                bw.newLine();
            }

            for (String s : pf) {
                bw.write(String.format("[Pass->Fail] %s", s));
                bw.newLine();
            }

        } catch (IOException ex) {
            System.out.println("Could not save patch information to " + outputFile.getAbsolutePath());
            System.out.println(ex.getMessage());
        }
    }
}

class GzoltarRunProcess implements Callable<Boolean> {

    public GZoltar gzoltar;

    public GzoltarRunProcess(GZoltar gzoltar) {
        this.gzoltar = gzoltar;
    }

    public synchronized Boolean call() {
        if (Thread.interrupted()) {
            return false;
        }
        gzoltar.run();
        return true;
    }
}
