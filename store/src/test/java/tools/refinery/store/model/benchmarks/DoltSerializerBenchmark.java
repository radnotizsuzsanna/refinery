package tools.refinery.store.model.benchmarks;

import org.junit.jupiter.api.io.TempDir;
import tools.refinery.store.model.*;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static tools.refinery.store.model.representation.TruthValue.*;

public class DoltSerializerBenchmark {
	private static final String DOLT_PATH = "C:\\Program Files\\Dolt\\bin\\dolt.exe";

	private static Process doltProcess;

	private static File tmpdir;

	private static final Thread shutdownHook = new Thread() {
		@Override
		public void run() {
			if (doltProcess != null && doltProcess.isAlive()) {
				doltProcess.destroyForcibly();
			}
		}
	};

	static void startDolt() throws IOException, InterruptedException {
		tmpdir = Files.createTempDirectory("dolt-benchmark").toFile();
		var initCommand = new ProcessBuilder(DOLT_PATH, "init")
			.directory(tmpdir)
			.redirectOutput(ProcessBuilder.Redirect.INHERIT)
			.redirectError(ProcessBuilder.Redirect.INHERIT)
			.start();
		boolean initFinished;
		try {
			initFinished = initCommand.waitFor(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			initCommand.destroyForcibly();
			throw e;
		}
		if (!initFinished) {
			initCommand.destroyForcibly();
			throw new IllegalStateException("Initializing dolt database timed out");
		}
		if (initCommand.exitValue() != 0) {
			throw new IllegalStateException("Initializing dolt database failed with exit code " +
				initCommand.exitValue());
		}
		var doltProcessBuilder = new ProcessBuilder(DOLT_PATH, "sql-server")
			.directory(tmpdir)
			.redirectOutput(ProcessBuilder.Redirect.INHERIT)
			.redirectError(ProcessBuilder.Redirect.INHERIT);
		Runtime.getRuntime().addShutdownHook(shutdownHook);
		doltProcess = doltProcessBuilder.start();
		// Wait until dolt starts listening for new connections.
		Thread.sleep(5000);
	}

	static void stopDolt() throws InterruptedException, IOException {
		if (doltProcess == null) {
			return;
		}
		doltProcess.destroy();
		boolean doltFinished;
		try {
			doltFinished = doltProcess.waitFor(10, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			doltProcess.destroyForcibly();
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
			throw e;
		}
		if (!doltFinished) {
			doltProcess.destroyForcibly();
			Runtime.getRuntime().removeShutdownHook(shutdownHook);
			throw new IllegalStateException("Stopping dolt timed out");
		}
		Runtime.getRuntime().removeShutdownHook(shutdownHook);

		try (var stream = Files.walk(tmpdir.toPath())) {
			//noinspection ResultOfMethodCallIgnored
			stream.sorted(Comparator.reverseOrder())
				.map(Path::toFile)
				.forEach(File::delete);
		}
		tmpdir = null;
	}

	static void serializeStore() throws SQLException {
		Relation<Boolean> person = new Relation<>("person", 1, Boolean.class, false);
		Relation<Integer> age = new Relation<>("age", 1, Integer.class, 0);
		Relation<Boolean> friend = new Relation<>("friend", 2, Boolean.class, false);
		Relation<TruthValue> girl = new Relation<>("girl", 1, TruthValue.class, UNKNOWN);

		ModelStore store = new ModelStoreImpl(Set.of(person, age, friend, girl));
		Model model = store.createModel();

		model.put(person, Tuple.of(0), true);
		model.put(person, Tuple.of(1), true);
		model.put(age, Tuple.of(0), 21);
		model.put(age, Tuple.of(1), 34);
		model.put(friend, Tuple.of(0, 1), true);
		model.put(girl, Tuple.of(0), TRUE);
		model.put(girl, Tuple.of(1), UNKNOWN);
		model.commit();
		model.put(person, Tuple.of(2), true);
		model.put(age, Tuple.of(2), 22);
		model.put(girl, Tuple.of(2), FALSE);
		model.put(friend, Tuple.of(0, 2), true);
		model.commit();

		//Sets the serializer strategy for every type int the model
		ModelSerializerWithDolt serializer = new ModelSerializerWithDolt();
		serializer.writeAllVersions(store);
	}




	public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException, InterruptedException {

		for (int i = 0; i<30 ;++i){
			startDolt();
			var init_time = System.currentTimeMillis();
			serializeStore();
			var duration = System.currentTimeMillis() - init_time;
			System.out.println(duration);
			stopDolt();
			System.gc();
			Thread.sleep(1000);
		}
	}
}

// dimenziók:
// hány eleme van a relációnak
// hány reláció van
// random seed
