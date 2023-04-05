package tools.refinery.store.model;

import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
import tools.refinery.store.map.internal.MapDelta;
import tools.refinery.store.map.internal.MapTransaction;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;
import tools.refinery.store.model.representation.TruthValue;

import java.sql.*;
import java.util.Map;

public class ModelSerializerWithDolt {
	Connection connection;
	public void write(ModelStore store) throws SQLException, ClassNotFoundException {
		connectToDataBase("Store");
		doltReset();
		if (store instanceof ModelStoreImpl impl) {
			for (Map.Entry<DataRepresentation<?, ?>, VersionedMapStore<?, ?>> entry : impl.stores.entrySet()) {
				DataRepresentation<?, ?> dataRepresentation = entry.getKey();
				if (dataRepresentation instanceof Relation<?> relation) {
					VersionedMapStore<?, ?> mapStore = entry.getValue();
					if (mapStore instanceof VersionedMapStoreDeltaImpl<?, ?> deltaStore) {

						try {
							writeRelation(relation, deltaStore);
						}
						catch (Exception e){
							clearDatabase(relation);
							writeRelation(relation,deltaStore);
						}

					} else {
						throw new UnsupportedOperationException("Only delta stores are supported!");
					}
				} else {
					throw new UnsupportedOperationException(
						"Only Relation representations are supported during serialization.");
				}
			}
		}
	}

	public <T> void clearDatabase(Relation<T> relation) throws SQLException {
		PreparedStatement ps = connection.prepareStatement("DROP TABLE "+relation.getName()+";");
		ps.execute();
	}
	public void connectToDataBase(String name) throws  SQLException {
		//Create connection
		String connectionUrl = "jdbc:mysql://localhost:3306/"+name+"?serverTimezone=UTC";
		connection = DriverManager.getConnection(connectionUrl, "root", "");

		//use database
		PreparedStatement ps = connection.prepareStatement("use "+name+";");
		ps.execute();
	}

	private <T> void writeRelation(Relation<T> relation, VersionedMapStoreDeltaImpl<?, ?> rawVersionedMapStore) throws SQLException {
		String name = relation.getName();
		PreparedStatement ps = null;
		String valueType = relation.getValueType().toString();
		String[] array =  valueType.split("\\.");
		valueType = array[array.length-1];

		//create table
		if(relation.getArity()==1){
			if(valueType.equals("TruthValue")){
				ps = connection.prepareStatement("CREATE TABLE "+name+"(" +
					"key1 int not null, " +
					"value1 TEXT not null, " +
					"primary key (key1));"
				);
			}
			else if(valueType.equals("Boolean")){
				ps = connection.prepareStatement("CREATE TABLE "+name+"(" +
					"key1 int not null, " +
					"value1 Boolean not null, " +
					"primary key (key1));"
				);
			}
			else if(valueType.equals("Integer")){
				ps = connection.prepareStatement("CREATE TABLE "+name+"(" +
					"key1 int not null, " +
					"value1 Int not null, " +
					"primary key (key1));"
				);
			}
		}
		else if(relation.getArity()==2 && valueType.equals("Boolean")){
			ps = connection.prepareStatement("CREATE TABLE "+name+"(" +
				"key1 int not null, " +
				"key2 int not null, " +
				"value1 Boolean not null, " +
				"primary key (key1, key2));"
			);
		}
		else{
			ps = connection.prepareStatement("");
		}
		ps.execute();

		doltAdd(name);
		doltCommit("Table " + name + " added to database");

		// Guaranteed to succeed by the precondition of this method.
		@SuppressWarnings("unchecked")
		VersionedMapStoreDeltaImpl<Tuple, T> versionedMapStore = (VersionedMapStoreDeltaImpl<Tuple, T>) rawVersionedMapStore;
		writeDeltaStore(relation, versionedMapStore);
	}

	private <T> void writeDeltaStore(Relation<T> relation, VersionedMapStoreDeltaImpl<Tuple,T> mapStore) throws SQLException {
		String valueType = relation.getValueType().toString();
		String[] array =  valueType.split("\\.");
		valueType = array[array.length-1];

		if(mapStore.getState(0) != null){
			for(int i = 0; i < mapStore.getStates().size(); i++){
				MapTransaction<Tuple, T> mapTransaction = mapStore.getState(i);
				MapDelta<Tuple, T>[] deltasOfTransaction = mapTransaction.deltas();

				int deltasLength = mapTransaction.deltas().length;

				for (int j = 0; j < deltasLength; j++) {
					MapDelta<Tuple, T> mapDelta = deltasOfTransaction[j];

					Tuple tuple = mapDelta.key();

					int arity = relation.getArity();
					PreparedStatement ps = null;
					//If it is the first occurrence of the delta -> insert
					if(mapDelta.oldValue() == null){
						if(arity == 1){
							ps = connection.prepareStatement("insert into "+ relation.getName()+" values(?, ?);");
							ps.setInt(1,tuple.get(0));
							switch (valueType) {
								case "TruthValue" -> {
									TruthValue truthValue = (TruthValue) mapDelta.newValue();
									ps.setString(2, truthValue.getName());
								}
								case "Boolean" -> {
									Boolean bool = (Boolean) mapDelta.newValue();
									ps.setBoolean(2, bool);
								}
								case "Integer" -> {
									Integer number = (Integer) mapDelta.newValue();
									ps.setInt(2, number);
								}
								default -> throw new UnsupportedOperationException("Only TruthValue, Boolean and Integer types are supported!");
							}

						}else if(arity == 2 && valueType.equals("Boolean")){
							ps = connection.prepareStatement("INSERT INTO "+ relation.getName()+" values(?, ?, ?);");
							ps.setInt(1,tuple.get(0));
							ps.setInt(2,tuple.get(1));
							Boolean bool = (Boolean) mapDelta.newValue();
							ps.setBoolean(3, bool);
						}
					}
					//If the delta already exist in the database -> update
					else{
						if(arity == 1){
							ps = connection.prepareStatement("UPDATE "+ relation.getName()+" SET value1 = ? WHERE key1 = ?;");
							ps.setInt(2, tuple.get(0));
							switch (valueType) {
								case "TruthValue" -> {
									TruthValue truthValue = (TruthValue) mapDelta.newValue();
									ps.setString(1, truthValue.getName());
								}
								case "Boolean" -> {
									Boolean bool = (Boolean) mapDelta.newValue();
									ps.setBoolean(1, bool);
								}
								case "Integer" -> {
									Integer number = (Integer) mapDelta.newValue();
									ps.setInt(1, number);
								}
								default -> throw new UnsupportedOperationException("Only TruthValue, Boolean and Integer types are supported!");
							}

						}else if(arity == 2 && valueType.equals("Boolean")){
							ps = connection.prepareStatement("UPDATE "+ relation.getName()+" SET value1 = ? WHERE key1 = ? AND key2 = ?;");
							ps.setInt(2,tuple.get(0));
							ps.setInt(3,tuple.get(1));
							Boolean bool = (Boolean) mapDelta.newValue();
							ps.setBoolean(1, bool);
						}
					}

					ps.execute();
				}
				doltAdd(relation.getName());
				doltCommit("Added data to table " + relation.getName());
			}
		}
		else{
			throw new UnsupportedOperationException("Tupples only supported with 1 or 2 arity!");
		}
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
		PreparedStatement ps = connection.prepareStatement("call dolt_reset('--hard', 'oe1jpm2pbqbb8nn09k95gigiqju64k44');");
		ps.execute();
	}
}
