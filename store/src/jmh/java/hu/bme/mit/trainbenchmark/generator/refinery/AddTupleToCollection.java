package hu.bme.mit.trainbenchmark.generator.refinery;

import java.util.Arrays;
import java.util.function.UnaryOperator;

import tools.refinery.store.model.Tuple;

public class AddTupleToCollection implements UnaryOperator<Object> {
	protected Tuple toAdd=null;
	
	public void setToAdd(Tuple toAdd) {
		this.toAdd = toAdd;
	}

	@Override
	public Object apply(Object old) {
		if(old == null) {
			return toAdd;
		} else if(old instanceof Tuple tupe) {
			return new Object[]{old, toAdd};
		} else {
			Object[] oldArray = (Object[]) old;
			Object[] newArray = Arrays.copyOf(oldArray, oldArray.length+1);
			newArray[newArray.length-1]=toAdd;
			return newArray;
		}
	}
}
