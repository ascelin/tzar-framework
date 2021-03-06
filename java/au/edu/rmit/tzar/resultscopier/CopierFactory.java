package au.edu.rmit.tzar.resultscopier;

import au.edu.rmit.tzar.db.RunDao;
import com.google.common.base.Optional;

import java.io.File;

/**
 * Creates Results copiers with certain characteristics.
 */
public class CopierFactory {
  public static final int RETRY_COUNT = 5;

  /**
   * Creates a new Asynchronous Results copier, wrapping the provided copier, and starts a new Thread
   * for the copier.
   *
   * @param copier the underlying copier (eg ScpResultsCopier, or FileResultsCopier)
   * @param updatesDb if we should wrap the copier in a DbUpdatingCopier, in order
   * to update the database when the copy is done.
   * @param retry if we should retry failed copies
   * @param runDao used to update the database is updatesDb is true, otherwise can be Optional.absent()
   * @param baseOutputPath the output path for the copier. used to update the database if updatesDb is true.
   * @return a new running AsyncResultsCopier
   */
  public AsyncResultsCopier createAsyncCopier(ResultsCopier copier, boolean updatesDb, boolean retry,
      Optional<RunDao> runDao, File baseOutputPath) {
    if (retry) {
      copier = new RetryingResultsCopier(copier, RETRY_COUNT);
    }
    if (updatesDb) {
      copier = new DbUpdatingResultsCopier(copier, runDao.get(), baseOutputPath);
    }
    AsyncResultsCopier asyncResultsCopier = new AsyncResultsCopier(copier);
    new Thread(asyncResultsCopier).start();
    return asyncResultsCopier;
  }
}
