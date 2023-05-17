package tools.refinery.store.model;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.tuple.Tuple;

public record DataRepresentationCursorPair<T>(AnySymbol dataRepresentation, Cursor<Tuple, T> cursor) {
}
