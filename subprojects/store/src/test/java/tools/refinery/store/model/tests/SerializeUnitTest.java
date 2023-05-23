package tools.refinery.store.model.tests;

import tools.refinery.store.map.tests.fuzz.SerializeModelWithDifferentTypesFuzzTest;

import java.io.IOException;

public class SerializeUnitTest {
	public static void main(String[] args) {
		System.out.println("Start");
		try {
			System.in.read();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		System.out.println("Started");
		var x = new SerializeModelWithDifferentTypesFuzzTest();
		x.runFuzzTest("Teljesitmenymeres", 0, 100, 10, 10,
				false, 10);
		System.out.println("Finish");
	}
}
