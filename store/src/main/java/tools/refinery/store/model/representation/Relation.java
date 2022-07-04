package tools.refinery.store.model.representation;

import java.util.Objects;

import tools.refinery.store.model.Tuple;
import tools.refinery.store.model.TupleHashProvider;

public class Relation<D> extends DataRepresentation<Tuple, D> {
	private final String name;
	private final int arity;

	public Relation(String name, int arity, D defaultValue) {
		super(TupleHashProvider.singleton(), defaultValue);
		this.name = name;
		this.arity = arity;
	}

	@Override
	public String getName() {
		return name;
	}

	public int getArity() {
		return arity;
	}

	@Override
	public boolean isValidKey(Tuple key) {
		if(key == null) {
			return false;
		} else return key.getSize() == getArity();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + Objects.hash(arity, name);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		Relation<?> other = (Relation<?>) obj;
		return arity == other.arity && Objects.equals(name, other.name);
	}
}
