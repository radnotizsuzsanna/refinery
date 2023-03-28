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
						writeRelation(relation, deltaStore);
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
	public void connectToDataBase(String name) throws ClassNotFoundException, SQLException {
		//Create connection
		String connectionUrl = "jdbc:mysql://localhost:3306/"+name+"?serverTimezone=UTC";
		connection = DriverManager.getConnection(connectionUrl, "root", "");

		PreparedStatement ps = connection.prepareStatement("use "+name+";");
		ps.execute();
	}

	private <T> void writeRelation(Relation<T> relation, VersionedMapStoreDeltaImpl<?, ?> versionedMapStore){
		String name = relation.getName();
	}

}
