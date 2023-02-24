package tools.refinery.store.query.tests;

import org.junit.jupiter.api.Test;
import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.Variable;
import tools.refinery.store.query.view.KeyOnlyRelationView;
import tools.refinery.store.representation.Symbol;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static tools.refinery.store.query.tests.QueryMatchers.structurallyEqualTo;

class StructurallyEqualToTest {
	@Test
	void flatEqualsTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var personView = new KeyOnlyRelationView<>(person);

		var expected = Dnf.builder("Expected").parameters(q).clause(personView.call(q)).build();
		var actual = Dnf.builder("Actual").parameters(p).clause(personView.call(p)).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void flatNotEqualsTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var personView = new KeyOnlyRelationView<>(person);

		var expected = Dnf.builder("Expected").parameters(q).clause(personView.call(q)).build();
		var actual = Dnf.builder("Actual").parameters(p).clause(personView.call(q)).build();

		var assertion = structurallyEqualTo(expected);
		assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
	}

	@Test
	void deepEqualsTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var personView = new KeyOnlyRelationView<>(person);

		var expected = Dnf.builder("Expected").parameters(q).clause(
				Dnf.builder("Expected2").parameters(p).clause(personView.call(p)).build().call(q)
		).build();
		var actual = Dnf.builder("Actual").parameters(q).clause(
				Dnf.builder("Actual2").parameters(p).clause(personView.call(p)).build().call(q)
		).build();

		assertThat(actual, structurallyEqualTo(expected));
	}

	@Test
	void deepNotEqualsTest() {
		var p = new Variable("p");
		var q = new Variable("q");
		var person = new Symbol<>("Person", 1, Boolean.class, false);
		var personView = new KeyOnlyRelationView<>(person);

		var expected = Dnf.builder("Expected").parameters(q).clause(
				Dnf.builder("Expected2").parameters(p).clause(personView.call(p)).build().call(q)
		).build();
		var actual = Dnf.builder("Actual").parameters(q).clause(
				Dnf.builder("Actual2").parameters(p).clause(personView.call(q)).build().call(q)
		).build();

		var assertion = structurallyEqualTo(expected);
		var error = assertThrows(AssertionError.class, () -> assertThat(actual, assertion));
		assertThat(error.getMessage(), containsString(" called from Expected "));
	}
}
