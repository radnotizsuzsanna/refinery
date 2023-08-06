/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.statecoding;

import org.junit.jupiter.api.Test;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.*;

class StateCoderBuildTest {
	Symbol<Boolean> person = new Symbol<>("Person", 1, Boolean.class, false);
	Symbol<Integer> age = new Symbol<>("age", 1, Integer.class, null);
	Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class, false);

	@Test
	void simpleStateCoderTest() {
		var store = ModelStore.builder()
				.symbols(person, age, friend)
				.with(StateCoderAdapter
						.builder())
				.build();

		var model = store.createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);
		assertNotNull(stateCoder);

		var personI = model.getInterpretation(person);
		var friendI = model.getInterpretation(friend);
		var ageI = model.getInterpretation(age);
		fill(personI, friendI, ageI);

		stateCoder.calculateStateCode();
	}

	@Test
	void excludeTest() {
		var store = ModelStore.builder()
				.symbols(person, age, friend)
				.with(StateCoderAdapter.builder()
						.exclude(person)
						.exclude(age))
				.build();

		var model = store.createEmptyModel();
		var stateCoder = model.getAdapter(StateCoderAdapter.class);
		assertNotNull(stateCoder);

		var personI = model.getInterpretation(person);
		var friendI = model.getInterpretation(friend);
		var ageI = model.getInterpretation(age);
		fill(personI, friendI, ageI);

		int code = stateCoder.calculateStateCode().modelCode();

		ageI.put(Tuple.of(1),3);
		assertEquals(code,stateCoder.calculateStateCode().modelCode());

		ageI.put(Tuple.of(1),null);
		assertEquals(code,stateCoder.calculateStateCode().modelCode());

		personI.put(Tuple.of(2),false);
		assertEquals(code,stateCoder.calculateStateCode().modelCode());
	}

	private static void fill(Interpretation<Boolean> personI, Interpretation<Boolean> friendI, Interpretation<Integer> ageI) {
		personI.put(Tuple.of(1), true);
		personI.put(Tuple.of(2), true);

		ageI.put(Tuple.of(1), 5);
		ageI.put(Tuple.of(2), 4);

		friendI.put(Tuple.of(1, 2), true);
	}
}