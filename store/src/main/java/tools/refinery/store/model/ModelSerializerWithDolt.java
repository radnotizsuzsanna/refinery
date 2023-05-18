package tools.refinery.store.model;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;

import java.sql.*;

public class ModelSerializerWithDolt {
	Connection connection;

	public void writeAllVersions(ModelStore store) throws SQLException {
		connectToDataBase("modelstore");
		var dataRepresentations = store.getDataRepresentations();
		for (DataRepresentation<?, ?> item : dataRepresentations) {
			writeRelation(item);
		}
		doltCommit("initial commit");

		var iterator = store.getStates().longIterator();
		while (iterator.hasNext()) {
			long version = iterator.next();
			mainBranch();
			newBranch(String.valueOf(version));
			writeVersion(store, version);
		}
	}

	public void writeVersion(ModelStore store, long version) throws SQLException {
		var model = store.createModel(version);
		var dataRepresentations = store.getDataRepresentations();
		boolean changed = false;
		for (DataRepresentation<?, ?> item : dataRepresentations) {
			if (!(item instanceof Relation<?> relation)) {
				throw new IllegalArgumentException("Only Relation representations are supported during serialization.");
			}
			changed = changed || writeDelta(relation, model);
		}
		if (changed) {
			doltCommit("Model state version " + version + " commited");
		}
	}

	private <T> void writeRelation(DataRepresentation<?, ?> dataRepresentation) throws SQLException {
		if (dataRepresentation instanceof Relation<?> relation) {
			String name = relation.getName();
			PreparedStatement ps;

			var createTableBuilder = new StringBuilder();
			createTableBuilder.append("CREATE TABLE ");
			createTableBuilder.append(name);
			createTableBuilder.append("(");
			int arity = relation.getArity();
			if (arity == 0) {
				throw new IllegalArgumentException("Arity 0 symbols are not currently supported");
			}
			for (int i = 0; i < arity; i++) {
				createTableBuilder.append("key");
				createTableBuilder.append(i);
				createTableBuilder.append(" int not null, ");
			}
			var valueType = relation.getValueType();
			if (valueType.equals(Boolean.class)) {
				if (!Boolean.FALSE.equals(relation.getDefaultValue())) {
					throw new IllegalArgumentException("Default value for Boolean symbols must be false");
				}
			} else if (valueType.equals(Integer.class) || valueType.equals(TruthValue.class)) {
				createTableBuilder.append("value int not null, ");
			} else {
				throw new IllegalArgumentException("Unsupported value type: " + valueType);
			}
			createTableBuilder.append("primary key (");
			for (int i = 0; i < arity; i++) {
				if (i != 0) {
					createTableBuilder.append(", ");
				}
				createTableBuilder.append("key");
				createTableBuilder.append(i);
			}
			createTableBuilder.append("));");

			ps = connection.prepareStatement(createTableBuilder.toString());

			ps.executeUpdate();

			doltAdd(name);
		} else {

			throw new UnsupportedOperationException(
				"Only Relation representations are supported during serialization.");
		}
	}

	private <V> boolean writeDelta(Relation<V> relation, Model model) throws SQLException {
		// Create prepared statement
		Cursor<Tuple, V> cursor = model.getAll(relation);
		if (!cursor.move()) {
			return false;
		}
		do {
			var key = cursor.getKey();
			var value = cursor.getValue();
			// Add key-value to batch insert
			// https://docs.oracle.com/javase/8/docs/api/java/sql/PreparedStatement.html#addBatch--

			PreparedStatement ps = null;

			int arity = relation.getArity();
			var valueType = relation.getValueType();
			int columns = valueType.equals(Boolean.class) ? arity : arity + 1;
			var insertBuilder = new StringBuilder();
			insertBuilder.append("insert into ");
			insertBuilder.append(relation.getName());
			insertBuilder.append(" values(");
			for (int i = 0; i < columns; i++) {
				if (i != 0) {
					insertBuilder.append(", ");
				}
				insertBuilder.append("?");
			}
			insertBuilder.append(");");
			ps = connection.prepareStatement(insertBuilder.toString());

			for (int i = 0; i < arity; i++) {
				ps.setInt(i + 1, key.get(i));
			}
			if (valueType.equals(Integer.class)) {
				ps.setInt(columns, (Integer) value);
			} else if (valueType.equals(TruthValue.class)) {
				ps.setInt(columns, ((TruthValue) value).ordinal());
			} else if (!valueType.equals(Boolean.class)) {
				throw new IllegalArgumentException("Unsupported value type: " + valueType);
			}

			ps.executeUpdate();
		} while (cursor.move());

		doltAdd(relation.getName());
		return true;
	}

	public void newBranch(String branchName) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("call dolt_checkout('-b',?);");
		ps.setString(1, branchName);
		ResultSet rs = ps.executeQuery();

		while (rs.next()) {
			String result = rs.getString(1);
			System.out.println("\nBranch result: " + result);
		}
	}

	public void mainBranch() throws SQLException {
		PreparedStatement ps = connection.prepareStatement("call dolt_checkout(?);");
		ps.setString(1, "main");
		ResultSet rs = ps.executeQuery();

		while (rs.next()) {
			String result = rs.getString(1);
			System.out.println("\nBranch result: " + result);
		}
	}

	public void connectToDataBase(String name) throws  SQLException {
		//Create connection
		String connectionUrl = "jdbc:mysql://localhost:3306/?serverTimezone=UTC";
		connection = DriverManager.getConnection(connectionUrl, "root", "");

		PreparedStatement ps1 = connection.prepareStatement("create database "+name+";");
		ps1.execute();

		//use database
		PreparedStatement ps2 = connection.prepareStatement("use "+name+";");
		ps2.execute();
	}

	private void doltAdd(String tableName) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("call dolt_add(?);");
		ps.setString(1, tableName);
		ResultSet rs = ps.executeQuery();

		while (rs.next()) {
			String result = rs.getString(1);
			System.out.println("\nAdd result: " + result);
		}
	}
	private void doltCommit(String message) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("call dolt_commit('-am', ?);");
		ps.setString(1, message);
		ResultSet rs = ps.executeQuery();

		while (rs.next()) {
			String result = rs.getString(1);
			System.out.println("\nCommit result: " + result);
		}
	}
	private void doltReset() throws SQLException {
		//TODO valtozoba kiszervezni
		PreparedStatement ps = connection.prepareStatement("call dolt_reset('--hard', 'qq58hll09k847k8c5tj154cr4era949p');");
		ps.execute();
	}
}
