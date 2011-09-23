package au.edu.rmit.tzar.commands;

import au.edu.rmit.tzar.ExecutableRun;
import au.edu.rmit.tzar.RunFactory;
import au.edu.rmit.tzar.RunnerFactory;
import au.edu.rmit.tzar.api.RdvException;
import au.edu.rmit.tzar.api.Run;
import au.edu.rmit.tzar.repository.CodeRepository;
import com.google.common.collect.Lists;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static au.edu.rmit.tzar.commands.CommandFlags.EXEC_LOCAL_RUNS_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.CREATE_RUNS_FLAGS;
import static au.edu.rmit.tzar.commands.SharedFlags.RUNNER_FLAGS;

/**
 * Execute a set of runs locally (ie without contacting the runs database).
 */
class ExecLocalRuns implements Command {
  private static Logger LOG = Logger.getLogger(ExecLocalRuns.class.getName());
  public static final Object[] FLAGS = new Object[]{EXEC_LOCAL_RUNS_FLAGS, CREATE_RUNS_FLAGS, RUNNER_FLAGS};

  private final RunFactory runFactory;
  private final int numRuns;
  private final File baseOutputPath;
  private final CodeRepository codeRepository;
  private final RunnerFactory runnerFactory;

  public ExecLocalRuns(File runSpecPath, int numRuns, RunFactory runFactory, File baseOutputPath,
      CodeRepository codeRepository, RunnerFactory runnerFactory) throws RdvException, IOException {
    this.numRuns = numRuns;
    this.baseOutputPath = baseOutputPath;
    this.codeRepository = codeRepository;
    this.runnerFactory = runnerFactory;
    // TODO(michaell): deal with this
//    runSpecPath = runSpecPath;
    this.runFactory = runFactory;
  }

  @Override
  public boolean execute() throws InterruptedException, RdvException {
    List<Run> runs = runFactory.createRuns(numRuns);
    List<Integer> failedIds = Lists.newArrayList();
    for (Run run : runs) {
      if (!executeRun(ExecutableRun.createExecutableRun(run, baseOutputPath, codeRepository,
          runnerFactory.loadRunner()))) {
        failedIds.add(run.getRunId());
      }
    }

    int count = runs.size();
    Level level;
    int failed = failedIds.size();
    boolean allSuccess = failed == 0;
    if (allSuccess) {
      level = Level.INFO;
    } else {
      level = Level.WARNING;
    }
    LOG.log(level, String.format("Executed %d runs: %d succeeded. %d failed", count, count - failed, failed));
    if (!allSuccess) {
      LOG.warning("Failed IDs were: " + failedIds);
    }
    return allSuccess;

//    if (projectSpecPath != null) {
//      return executeProjectSpec();
//    } else if (runSpecPath != null) {
//      return executeRunSpec();
//    } else {
//      throw new IllegalStateException("Either projectSpec or runSpec must be set.");
//    }
  }

  //  private boolean executeRunSpec() throws RdvException {
//    try {
//      Parameters parameters = jsonParser.parametersFromJson(runSpecPath);
//      ExecutableRun run = runFactory.createRun(baseRunId, "local run", parameters, revision, commandFlags, runset);
//      return executeRun(run);
//    } catch (FileNotFoundException e) {
//      throw new RdvException(e);
//    }
//  }
//
  private boolean executeRun(ExecutableRun run) throws RdvException {
    if (run.execute()) {
      LOG.info("Run " + run.getRunId() + " succeeded.");
      return true;
    } else {
      LOG.warning("Run " + run.getRunId() + " failed.");
      return false;
    }
  }
}
