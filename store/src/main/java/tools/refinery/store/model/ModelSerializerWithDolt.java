package tools.refinery.store.model;

import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.map.VersionedMapStoreDeltaImpl;
import tools.refinery.store.model.representation.DataRepresentation;
import tools.refinery.store.model.representation.Relation;

import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class ModelSerializerWithDolt {
	Connection connection;
	public void write(ModelStore store) throws SQLException, ClassNotFoundException {
		connectToDataBase("Store");
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
	public void connectToDataBase(String name) throws ClassNotFoundException, SQLException {
		//Create connection
		String connectionUrl = "jdbc:mysql://localhost:3306/"+name+"?serverTimezone=UTC";
		connection = DriverManager.getConnection(connectionUrl, "root", "");

		PreparedStatement ps = connection.prepareStatement("use "+name+";");
		ps.execute();
	}

	private <T> void writeRelation(Relation<T> relation, VersionedMapStoreDeltaImpl<?, ?> versionedMapStore) throws SQLException {
		String name = relation.getName();
		PreparedStatement ps = null;
		String valueType = relation.getValueType().toString();
		String[] array =  valueType.split("\\.");
		valueType = array[array.length-1];

		if(relation.getArity()==1){
			if(valueType.equals("TruthValue")){
				ps = connection.prepareStatement("CREATE TABLE "+name+"(" +
					"key1 int not null, " +
					"value1 TEXT not null, " +
					"primary key (key1));"
				);
			}
			else{
				ps = connection.prepareStatement("CREATE TABLE "+name+"(" +
					"key1 int not null, " +
					"value1 "+valueType+" not null, " +
					"primary key (key1));"
				);
			}

		}
		else if(relation.getArity()==2){
			ps = connection.prepareStatement("CREATE TABLE "+name+"(" +
				"key1 int not null, " +
				"key2 int not null, " +
				"value1 int not null, " +
				"primary key (key1, key2));"
			);
		}
		else{
			ps = connection.prepareStatement("");
		}
		ps.execute();
	}

}
