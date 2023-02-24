package tools.refinery.store.query.literal;

import tools.refinery.store.query.Variable;
import tools.refinery.store.query.equality.LiteralEqualityHelper;
import tools.refinery.store.query.substitution.Substitution;

import java.util.Set;

public enum BooleanLiteral implements PolarLiteral<BooleanLiteral> {
	TRUE(true),
	FALSE(false);

	private final boolean value;

	BooleanLiteral(boolean value) {
		this.value = value;
	}

	@Override
	public void collectAllVariables(Set<Variable> variables) {
		// No variables to collect.
	}

	@Override
	public Literal substitute(Substitution substitution) {
		// No variables to substitute.
		return this;
	}

	@Override
	public LiteralReduction getReduction() {
		return value ? LiteralReduction.ALWAYS_TRUE : LiteralReduction.ALWAYS_FALSE;
	}

	@Override
	public BooleanLiteral negate() {
		return fromBoolean(!value);
	}

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		return equals(other);
	}

	@Override
	public String toString() {
		return Boolean.toString(value);
	}

	public static BooleanLiteral fromBoolean(boolean value) {
		return value ? TRUE : FALSE;
	}
}
